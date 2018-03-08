package com.distelli.europa.models;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.registry.ContainerRepoNotFoundException;
import com.distelli.europa.registry.CopyImageBetweenRepos;
import com.distelli.europa.registry.ManifestNotFoundException;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Provider;
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
    private Provider<CopyImageBetweenRepos> _copyImageBetweenReposProvider;
    @Inject
    private Injector _injector;

    @Override
    public Optional<PromotedImage> execute(PromotedImage promotedImage) throws IOException {
        if (_repoDb == null) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }

        ContainerRepo sourceRepo = promotedImage.getRepo();
        String sourceTag = promotedImage.getTag();
        String manifestDigestSha = promotedImage.getManifestDigestSha();

        // sourceRepo should never be null in normal use
        if (sourceRepo == null) {
            throw new IllegalArgumentException("Source repository must not be null");
        }
        // sourceTag should never be null in normal use
        if (sourceTag == null) {
            throw new IllegalArgumentException("Source tag must not be null");
        }
        // TODO: if manifestDigestSha is null, we should issue a "DELETE"
        if (manifestDigestSha == null) {
            log.debug("Tag delete is not implemented");
            return (Optional.of(promotedImage));
        }
        // Not configured? Ignore...
        if (destinationContainerRepoId == null ||
            destinationContainerRepoDomain == null) {
            log.error(String.format("PipelineComponentId=%s has null destinationContainerRepoId or destinationContainerRepoDomain", getId()));
            return (Optional.of(promotedImage));
        }
        // From the same repo? Ignore...
        if (destinationContainerRepoId.equals(sourceRepo.getId()) && !tag.equals(sourceTag)) {
            log.error(String.format("PipelineComponentId=%s pushes to itself!?", getId()));
            return (Optional.of(promotedImage));
        }

        ContainerRepo destinationRepo = _repoDb.getRepo(destinationContainerRepoDomain, destinationContainerRepoId);
        String destinationTag = (tag == null) ? sourceTag : tag;

        // destinationRepo will be null when a repo referenced by a pipeline is deleted.
        if (destinationRepo == null) {
            ContainerRepoNotFoundException e = new ContainerRepoNotFoundException(destinationContainerRepoDomain,
                                                                                  null,
                                                                                  destinationContainerRepoId);
            log.debug(String.format("Failed to look up repo for PipelineComponentId=%s", getId()), e);
            return (Optional.of(promotedImage));
        }

        try {
            _copyImageBetweenReposProvider.get()
                .sourceRepo(sourceRepo)
                .sourceReference(manifestDigestSha)
                .destinationRepo(destinationRepo)
                .destinationTag(destinationTag)
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
