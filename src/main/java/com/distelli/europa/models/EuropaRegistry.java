package com.distelli.europa.models;

import com.distelli.europa.db.RegistryBlobDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.guice.ObjectKeyFactoryProvider;
import com.distelli.europa.guice.ObjectStoreProvider;
import com.distelli.gcr.models.GcrBlobMeta;
import com.distelli.gcr.models.GcrBlobReader;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import com.distelli.gcr.models.GcrManifestMeta;
import com.distelli.objectStore.ObjectKey;
import com.distelli.utils.CountingInputStream;
import com.distelli.utils.ResettableInputStream;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.distelli.europa.Constants.DOMAIN_ZERO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class EuropaRegistry implements Registry {

    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    private ObjectKeyFactoryProvider _objectKeyFactoryProvider;
    @Inject
    private ObjectStoreProvider _objectStoreProvider;
    @Inject
    private RegistryBlobDb _blobDb;

    private ContainerRepo repo;

    public EuropaRegistry(ContainerRepo repo) {
        this.repo = repo;
    }

    @Override
    public GcrManifest getManifest(String repository, String reference) throws IOException {
        if (!repo.getName().equals(repository)) {
            throw new IllegalArgumentException(String.format("Expected repo.name=%s, but got repository=%s",
                                                             repo.getName(),
                                                             repository));
        }
        RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(repo.getDomain(), repo.getId(), reference);
        if (null == manifest) {
            return null;
        }
        ObjectKey key = _objectKeyFactoryProvider.get()
            .forRegistryManifest(manifest.getManifestId());
        byte[] binary = _objectStoreProvider.get().get(key);
        if (null == binary) {
            return null;
        }
        String manifestContent = new String(binary, UTF_8);
        return new GcrManifest() {
            public String getMediaType() {
                return manifest.getContentType();
            }

            public String toString() {
                return manifestContent;
            }

            public List<String> getReferencedDigests() {
                return new ArrayList<>(manifest.getDigests());
            }
        };
    }

    @Override
    public <T> T getBlob(String repository, String digest, GcrBlobReader<T> reader) throws IOException {
        RegistryBlob blob = _blobDb.getRegistryBlobByDigest(digest.toLowerCase());
        if (null == blob) {
            return reader.read(new ByteArrayInputStream(new byte[0]), null);
        }
        ObjectKey key = _objectKeyFactoryProvider.get()
            .forRegistryBlobId(blob.getBlobId());
        return _objectStoreProvider.get()
            .get(key, (meta, in) -> reader.read(in,
                                                GcrBlobMeta.builder()
                                                    .digest(digest)
                                                    .length(meta.getContentLength())
                                                    .build()));
    }

    @Override
    public GcrBlobUpload createBlobUpload(String repository, String digest, String fromRepository) throws IOException {
        RegistryBlob blob = _blobDb.getRegistryBlobByDigest(digest.toLowerCase());
        return GcrBlobUpload.builder()
            .complete(null != blob)
            .digest(digest)
            .mediaType(null != blob ? blob.getMediaType() : null)
            .build();
    }

    @Override
    public GcrBlobMeta blobUploadChunk(GcrBlobUpload blobUpload, InputStream chunk, Long chunkLength, String digest) throws IOException {
        // TODO: Get the pipeline domain!
        RegistryBlob blob = _blobDb.newRegistryBlob(DOMAIN_ZERO);
        ObjectKey key = _objectKeyFactoryProvider.get()
            .forRegistryBlobId(blob.getBlobId());
        if (null == chunkLength) {
            // Buffer on disk to determine object size:
            chunk = new ResettableInputStream(chunk);
            CountingInputStream counter = new CountingInputStream(chunk);
            byte[] buff = new byte[1024 * 1024];
            while (counter.read(buff) > 0) {
                ;
            }
            chunkLength = counter.getCount();
            chunk.reset();
        }
        MessageDigest md = getSha256();
        chunk = new DigestInputStream(chunk, md);
        _objectStoreProvider.get().put(key, chunkLength, chunk);
        String expectDigest = "sha256:" + printHexBinary(md.digest()).toLowerCase();
        if (!digest.equals(expectDigest)) {
            throw new IllegalArgumentException(String.format("Computed digest=%s, but declared digest=%s",
                                                             expectDigest,
                                                             digest));
        }
        _blobDb.finishUpload(blob.getBlobId(),
                             null,
                             digest,
                             chunkLength,
                             blobUpload.getMediaType());
        return GcrBlobMeta.builder()
            .length(chunkLength)
            .digest(digest)
            .build();
    }

    private MessageDigest getSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public GcrManifestMeta putManifest(String repository, String reference, GcrManifest gcrManifest) throws IOException {
        if (!repo.getName().equals(repository)) {
            throw new IllegalArgumentException(String.format("Expected repo.name=%s, but got repository=%s",
                                                             repo.getName(),
                                                             repository));
        }
        byte[] binary = gcrManifest.toString().getBytes(UTF_8);
        MessageDigest md = getSha256();
        String digest = "sha256:" + printHexBinary(md.digest(binary)).toLowerCase();

        ObjectKey key = _objectKeyFactoryProvider.get()
            .forRegistryManifest(digest);
        _objectStoreProvider.get().put(key, binary);

        RegistryManifest manifest = RegistryManifest.builder()
            // TODO: Get the pipeline domain!
            .uploadedBy(DOMAIN_ZERO)
            .contentType(gcrManifest.getMediaType())
            .manifestId(digest)
            .domain(repo.getDomain())
            .containerRepoId(repo.getId())
            .tag(reference)
            .digests(gcrManifest.getReferencedDigests())
            .pushTime(System.currentTimeMillis())
            .build();
        _manifestDb.put(manifest);
        // TODO: Should we trigger other pipelines? .. perhaps we should move pipeline triggers
        // for europa repositories into the monitor stuff. This way we can avoid pipeline
        // execution overlap.
        return GcrManifestMeta.builder()
            .digest(digest)
            .location(null) // unknown...
            .mediaType(manifest.getContentType())
            .build();
    }
}
