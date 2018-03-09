package com.distelli.europa.registry;

import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.DockerHubRegistry;
import com.distelli.europa.models.EcrRegistry;
import com.distelli.europa.models.EuropaRegistry;
import com.distelli.europa.models.GcrRegistry;
import com.distelli.europa.models.Registry;

import javax.inject.Inject;

public class RegistryFactory {
    @Inject
    private DockerHubRegistry.Factory _dockerHubRegistryFactory;
    @Inject
    private EcrRegistry.Factory _ecrRegistryFactory;
    @Inject
    private EuropaRegistry.Factory _europaRegistryFactory;
    @Inject
    private GcrRegistry.Factory _gcrRegistryFactory;

    /**
     * Get a Registry object which can connect to the registry for the specified {@link ContainerRepo}
     *
     * @param repo the repository we want to connect to
     * @param isPush {@code true} if we need to push to the repository, {@code false} if we don't
     * @param crossBlobMountFrom the name of the source repository, if we're pushing to this registry
     * @return a Registry object for the requested registry
     */
    public Registry createRegistry(ContainerRepo repo, Boolean isPush, String crossBlobMountFrom) {
        switch (repo.getProvider()) {
            case DOCKERHUB:
                if (null == crossBlobMountFrom) {
                    return _dockerHubRegistryFactory.create(repo, isPush);
                } else {
                    return _dockerHubRegistryFactory.create(repo, isPush, crossBlobMountFrom);
                }
            case ECR:
                return _ecrRegistryFactory.create(repo);
            case EUROPA:
                return _europaRegistryFactory.create(repo);
            case GCR:
                return _gcrRegistryFactory.create(repo);
            default:
                throw new UnsupportedOperationException(String.format("No Registry implementation for provider=%s",
                                                                      repo.getProvider()));
        }
    }
}
