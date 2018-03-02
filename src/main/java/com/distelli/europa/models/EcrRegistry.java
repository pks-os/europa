package com.distelli.europa.models;

import com.distelli.europa.clients.ECRClient;
import com.distelli.gcr.GcrClient;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;

public class EcrRegistry extends RemoteRegistry {
    public interface Factory {
        EcrRegistry create(ContainerRepo repo);
    }

    @Inject
    public EcrRegistry(ContainerRepo repo) throws IOException {
        super(repo, EcrRegistry::getClient);
    }

    private static GcrClient getClient(Provider<GcrClient.Builder> gcrClientBuilderProvider, ContainerRepo repo, RegistryCred cred) {
        AuthorizationToken token = new ECRClient(cred).getAuthorizationToken(repo.getRegistryId());
        return gcrClientBuilderProvider.get()
            .gcrCredentials(() -> "Basic "+token.getToken())
            .endpoint(token.getEndpoint())
            .build();
    }
}
