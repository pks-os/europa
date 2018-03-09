package com.distelli.europa.models;

import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.models.GcrBlobMeta;
import com.distelli.gcr.models.GcrBlobReader;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import com.distelli.gcr.models.GcrManifestMeta;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;

public abstract class RemoteRegistry implements Registry {
    @Inject
    private RegistryCredsDb _registryCredsDb;

    private GcrClient client;
    @Getter
    private ContainerRepo repo;
    private RegistryCred cred = null;

    protected RemoteRegistry(ContainerRepo repo) {
        if (repo == null) {
            throw new IllegalArgumentException("Repo cannot be empty");
        }
        this.repo = repo;
    }

    abstract protected GcrClient createClient() throws IOException;

    protected final GcrClient getClient() throws IOException {
        if (client == null) {
            client = createClient();
        }
        return client;
    }

    protected final RegistryCred getCred() {
        if (cred == null && repo.getCredId() != null) {
            cred = _registryCredsDb.getCred(repo.getDomain(), repo.getCredId());
        }
        return cred;
    }

    @Override
    public GcrManifest getManifest(String repository, String reference) throws IOException {
        return getClient().getManifest(repository, reference, "application/vnd.docker.distribution.manifest.v2+json");
    }

    @Override
    public <T> T getBlob(String repository, String digest, GcrBlobReader<T> reader) throws IOException {
        return getClient().getBlob(repository, digest, reader);
    }

    @Override
    public GcrBlobUpload createBlobUpload(String repository, String digest, String fromRepository) throws IOException {
        return getClient().createBlobUpload(repository, digest, fromRepository);
    }

    @Override
    public GcrBlobMeta blobUploadChunk(GcrBlobUpload blobUpload, InputStream chunk, Long chunkLength, String digest) throws IOException {
        return getClient().blobUploadChunk(blobUpload, chunk, chunkLength, digest);
    }

    @Override
    public GcrManifestMeta putManifest(String repository, String reference, GcrManifest manifest) throws IOException {
        return getClient().putManifest(repository, reference, manifest);
    }

    public interface GcrClientGenerator {
        GcrClient createClient(Provider<GcrClient.Builder> gcrClientBuilderProvider,
                               ContainerRepo repo,
                               RegistryCred cred)
            throws IOException;
    }
}
