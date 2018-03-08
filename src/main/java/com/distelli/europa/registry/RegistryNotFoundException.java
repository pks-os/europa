package com.distelli.europa.registry;

import com.distelli.europa.models.RegistryProvider;

import javax.persistence.EntityNotFoundException;

public class RegistryNotFoundException extends EntityNotFoundException {
    /**
     * Exception for being unable to find or connect to the requested container registry
     *
     * @param provider the registry provider we attempted to connect to
     * @param repoName the name of the repository we attempted to connect to
     */
    public RegistryNotFoundException(RegistryProvider provider, String repoName) {
        super(String.format("Could not find registry for repo %s with provider %s",
                             repoName,
                             provider.toString()));
    }
}
