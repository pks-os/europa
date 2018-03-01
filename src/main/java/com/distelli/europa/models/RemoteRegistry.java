package com.distelli.europa.models;

import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.models.GcrBlobMeta;
import com.distelli.gcr.models.GcrBlobReader;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import com.distelli.gcr.models.GcrManifestMeta;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.InputStream;

public abstract class RemoteRegistry implements Registry {
    @Inject
    private Provider<GcrClient.Builder> _gcrClientBuilderProvider;
    @Inject
    protected RegistryCredsDb _registryCredsDb = null;

    protected GcrClient client;

    public RemoteRegistry(ContainerRepo repo, GcrClientGenerator gcrClientGenerator) throws IOException {
        RegistryCred cred = null;
        if ( null != repo.getCredId() ) {
            cred = _registryCredsDb.getCred(repo.getDomain(), repo.getCredId());
        }
        this.client = gcrClientGenerator.getClient(_gcrClientBuilderProvider, repo, cred);
    }

    @Override
    public GcrManifest getManifest(String repository, String reference) throws IOException {
        return client.getManifest(repository, reference, "application/vnd.docker.distribution.manifest.v2+json");
    }

    @Override
    public <T> T getBlob(String repository, String digest, GcrBlobReader<T> reader) throws IOException {
        return client.getBlob(repository, digest, reader);
    }

    @Override
    public GcrBlobUpload createBlobUpload(String repository, String digest, String fromRepository) throws IOException {
        return client.createBlobUpload(repository, digest, fromRepository);
    }

    @Override
    public GcrBlobMeta blobUploadChunk(GcrBlobUpload blobUpload, InputStream chunk, Long chunkLength, String digest) throws IOException {
        return client.blobUploadChunk(blobUpload, chunk, chunkLength, digest);
    }

    @Override
    public GcrManifestMeta putManifest(String repository, String reference, GcrManifest manifest) throws IOException {
        return client.putManifest(repository, reference, manifest);
    }

    protected interface GcrClientGenerator {
        GcrClient getClient(Provider<GcrClient.Builder> gcrClientBuilderProvider,
                            ContainerRepo repo,
                            RegistryCred cred)
            throws IOException;
    }
}
