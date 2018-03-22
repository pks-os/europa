package com.distelli.europa.models;

import com.distelli.gcr.GcrClient;
import com.distelli.gcr.GcrRegion;
import com.distelli.gcr.auth.GcrServiceAccountCredentials;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.inject.Inject;

public class GcrRegistry extends RemoteRegistry {
    public interface Factory {
        GcrRegistry create(ContainerRepo repo);
    }

    @Inject
    private GcrClient.Builder _gcrClientBuilder;

    @AssistedInject
    public GcrRegistry(@Assisted ContainerRepo repo) {
        super(repo);
    }

    @Override
    protected GcrClient createClient() {
        return _gcrClientBuilder.gcrCredentials(new GcrServiceAccountCredentials(getCred().getSecret()))
            .gcrRegion(GcrRegion.getRegionByEndpoint(getCred().getRegion()))
            .build();
    }
}
