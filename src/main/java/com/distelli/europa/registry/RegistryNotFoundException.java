package com.distelli.europa.registry;

import com.distelli.europa.models.RegistryProvider;

public class RegistryNotFoundException extends Exception {
    public RegistryNotFoundException(RegistryProvider provider, String repoName) {
        super(formatMessage(provider, repoName));
    }

    public RegistryNotFoundException(RegistryProvider provider, String repoName, Throwable throwable) {
        super(formatMessage(provider, repoName), throwable);
    }

    private static String formatMessage(RegistryProvider provider, String repoName) {
        return String.format("Could not find registry for repo %s with provider %s",
                             repoName,
                             provider.toString());
    }
}
