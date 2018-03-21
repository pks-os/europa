package com.distelli.europa.models;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.registry.ContainerRepoNotFoundException;
import com.distelli.europa.registry.CopyImageBetweenRepos;
import com.distelli.europa.registry.ManifestNotFoundException;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

/**
 * Pipeline component that copies from one repository to another.
 */
@Log4j
@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
@AllArgsConstructor
public class PCCopyToRepository extends PipelineComponent {
    private static final ObjectMapper OM = new ObjectMapper();
    private String destinationContainerRepoDomain;
    private String destinationContainerRepoId;
    private String tag;
    private Long lastExecutionTime;
    private ExecutionStatus lastExecutionStatus;

    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private CopyImageBetweenRepos.Builder _copyImageBetweenReposBuilder;

    @Override
    public Optional<PromotedImage> execute(PromotedImage promotedImage) throws IOException {
        if (null == _repoDb) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }

        ContainerRepo sourceRepo = promotedImage.getRepo();
        String sourceTag = promotedImage.getTag();
        String manifestDigestSha = promotedImage.getManifestDigestSha();

        // sourceRepo should never be null in normal use
        if (null == sourceRepo) {
            throw new IllegalArgumentException("Source repository must not be null");
        }
        // sourceTag should never be null in normal use
        if (null == sourceTag) {
            throw new IllegalArgumentException("Source tag must not be null");
        }
        // TODO: if manifestDigestSha is null, we should issue a "DELETE"
        if (null == manifestDigestSha) {
            log.debug("Tag delete is not implemented");
            return (Optional.of(promotedImage));
        }
        // Not configured? Ignore...
        if (null == destinationContainerRepoId ||
            null == destinationContainerRepoDomain) {
            log.error(String.format("PipelineComponentId=%s has null destinationContainerRepoId or destinationContainerRepoDomain", getId()));
            return (Optional.of(promotedImage));
        }
        // From the same repo? Ignore...
        if (destinationContainerRepoId.equals(sourceRepo.getId()) && !tag.equals(sourceTag)) {
            log.error(String.format("PipelineComponentId=%s pushes to itself!?", getId()));
            return (Optional.of(promotedImage));
        }

        ContainerRepo destinationRepo = _repoDb.getRepo(destinationContainerRepoDomain, destinationContainerRepoId);
        String destinationTag = (null == tag) ? sourceTag : tag;

        // destinationRepo will be null when a repo referenced by a pipeline is deleted.
        if (null == destinationRepo) {
            ContainerRepoNotFoundException e = new ContainerRepoNotFoundException(destinationContainerRepoDomain,
                                                                                  null,
                                                                                  destinationContainerRepoId);
            log.debug(String.format("Failed to look up repo for PipelineComponentId=%s", getId()), e);
            return (Optional.of(promotedImage));
        }

        try {
            _copyImageBetweenReposBuilder
                .sourceRepo(sourceRepo)
                .destinationRepo(destinationRepo)
                .sourceReference(manifestDigestSha)
                .destinationTag(destinationTag)
                .build()
                .run();
        } catch (ManifestNotFoundException e) {
            // Manifest could have been deleted, we could have received bad input, etc.
            log.error(String.format("Failed to find manifest when evaluating PipelineComponentId=%s", getId()), e);
            return (Optional.of(promotedImage));
        }
        return (Optional.of(new PromotedImage(destinationRepo, destinationTag, manifestDigestSha)));
    }

    @Override
    public void validate(String key) {
        if (null == _repoDb) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }

        if (null == destinationContainerRepoDomain) {
            throw new AjaxClientException(
                "Missing Param '" + key + ".destinationContainerRepoDomain' in request",
                JsonError.Codes.MissingParam,
                400);
        }
        if (null == destinationContainerRepoId) {
            throw new AjaxClientException(
                "Missing Param '" + key + ".destinationContainerRepoId' in request",
                JsonError.Codes.MissingParam,
                400);
        }
        ContainerRepo destinationRepo = _repoDb.getRepo(destinationContainerRepoDomain, destinationContainerRepoId);
        if (null == destinationRepo) {
            throw new AjaxClientException(String.format("Container repo not found for domain=%s and id=%s",
                                                        destinationContainerRepoDomain,
                                                        destinationContainerRepoId),
                                          JsonError.Codes.BadParam,
                                          400);
        }
        if (destinationRepo.isMirror()) {
            throw new AjaxClientException("Cannot add a mirror repository to a pipeline",
                                          JsonError.Codes.BadParam,
                                          400);
        }
    }

    protected PCCopyToRepository(String id, String destinationContainerRepoDomain, String destinationContainerRepoId, String tag, Long lastExecutionTime, ExecutionStatus lastExecutionStatus) {
        super(id);
        this.destinationContainerRepoDomain = destinationContainerRepoDomain;
        this.destinationContainerRepoId = destinationContainerRepoId;
        this.tag = tag;
        this.lastExecutionTime = lastExecutionTime;
        this.lastExecutionStatus = lastExecutionStatus;
    }

    public static class Builder<T extends Builder<T>> extends PipelineComponent.Builder<T> {
        protected String destinationContainerRepoDomain;
        protected String destinationContainerRepoId;
        protected String tag;
        protected Long lastExecutionTime;
        protected ExecutionStatus lastExecutionStatus;

        public T destinationContainerRepoDomain(String destinationContainerRepoDomain) {
            this.destinationContainerRepoDomain = destinationContainerRepoDomain;
            return self();
        }

        public T destinationContainerRepoId(String destinationContainerRepoId) {
            this.destinationContainerRepoId = destinationContainerRepoId;
            return self();
        }

        public T tag(String tag) {
            this.tag = tag;
            return self();
        }

        public T lastExecutionTime(Long lastExecutionTime) {
            this.lastExecutionTime = lastExecutionTime;
            return self();
        }

        public T lastExecutionStatus(ExecutionStatus lastExecutionStatus) {
            this.lastExecutionStatus = lastExecutionStatus;
            return self();
        }

        public PCCopyToRepository build() {
            return new PCCopyToRepository(id, destinationContainerRepoDomain, destinationContainerRepoId, tag, lastExecutionTime, lastExecutionStatus);
        }
    }

    public static Builder<?> builder() {
        return new Builder();
    }
}
