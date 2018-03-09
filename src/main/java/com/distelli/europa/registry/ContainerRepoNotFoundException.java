package com.distelli.europa.registry;

import javax.persistence.EntityNotFoundException;

public class ContainerRepoNotFoundException extends EntityNotFoundException {
    /**
     * Exception for being unable to find the requested container repo
     *
     * @param repoDomain the domain used to look up the requested repo
     * @param repoName the repo name used to look up the requested repo, if applicable
     * @param repoId the repo id used to look up the requested repo, if applicable
     */
    public ContainerRepoNotFoundException(String repoDomain, String repoName, String repoId) {
        super(formatMessage(repoDomain, repoName, repoId));
    }

    /**
     * Exception for being unable to find the requested container repo
     *
     * @param repoDomain the domain used to look up the requested repo
     * @param repoName the repo name used to look up the requested repo, if applicable
     * @param repoId the repo id used to look up the requested repo, if applicable
     * @param throwable the cause of the exception
     */
    public ContainerRepoNotFoundException(String repoDomain,
                                          String repoName,
                                          String repoId,
                                          Throwable throwable) {
        super(formatMessage(repoDomain, repoName, repoId));
        this.initCause(throwable);
    }

    private static String formatMessage(String repoDomain, String repoName, String repoId) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("No container repository found with domain %s", repoDomain));
        if (repoName != null) {
            message.append(String.format(" and name %s", repoName));
        }
        if (repoId != null) {
            message.append(String.format(" and id %s", repoId));
        }
        return message.toString();
    }
}
