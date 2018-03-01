package com.distelli.europa.registry;

public class ManifestNotFoundException extends Exception {
    public ManifestNotFoundException(String repoName, String manifestReference) {
        super(formatMessage(repoName, manifestReference));
    }

    public ManifestNotFoundException(String repoName, String manifestReference, Throwable throwable) {
        super(formatMessage(repoName, manifestReference), throwable);
    }

    private static String formatMessage(String repoName, String manifestReference) {
        return String.format("No manifest found for reference %s in repository %s",
                             manifestReference,
                             repoName);
    }
}
