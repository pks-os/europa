package com.distelli.europa.models;

import com.distelli.europa.clients.ECRClient;
import com.distelli.gcr.GcrClient;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class EcrRegistry extends RemoteRegistry {
    public interface Factory {
        EcrRegistry create(ContainerRepo repo);
    }

    @AssistedInject
    public EcrRegistry(@Assisted ContainerRepo repo) {
        super(repo);
    }

    @Override
    protected GcrClient createClient() {
        RegistryCred cred = getCred();
        AuthorizationToken token = new ECRClient(cred).getAuthorizationToken(getRepo().getRegistryId());
        return _gcrClientBuilder.gcrCredentials(() -> "Basic "+token.getToken())
            .endpoint(token.getEndpoint())
            .build();
    }
}
