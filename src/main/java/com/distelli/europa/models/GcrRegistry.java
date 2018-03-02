package com.distelli.europa.models;

import com.distelli.gcr.GcrClient;
import com.distelli.gcr.GcrRegion;
import com.distelli.gcr.auth.GcrServiceAccountCredentials;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;

public class GcrRegistry extends RemoteRegistry {
    public interface Factory {
        GcrRegistry create(ContainerRepo repo);
    }

    @Inject
    public GcrRegistry(ContainerRepo repo) throws IOException {
        super(repo, GcrRegistry::getClient);
    }

    private static GcrClient getClient(Provider<GcrClient.Builder> gcrClientBuilderProvider, ContainerRepo repo, RegistryCred cred) {
        return gcrClientBuilderProvider.get()
            .gcrCredentials(new GcrServiceAccountCredentials(cred.getSecret()))
            .gcrRegion(GcrRegion.getRegion(cred.getRegion()))
            .build();
    }
}
