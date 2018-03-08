package com.distelli.europa.registry;

import javax.persistence.EntityNotFoundException;

public class ManifestNotFoundException extends EntityNotFoundException {
    /**
     * Exception for being unable to find the requested container image manifest
     *
     * @param repoName the repo name used to look up the manifest
     * @param manifestReference the reference (digest or tag) used to look up the manifest
     */
    public ManifestNotFoundException(String repoName, String manifestReference) {
        super(String.format("No manifest found for reference %s in repository %s",
                             manifestReference,
                             repoName));
    }
}
