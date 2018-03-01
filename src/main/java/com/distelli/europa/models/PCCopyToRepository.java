package com.distelli.europa.models;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.RegistryBlobDb;
import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.util.ObjectKeyFactory;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import com.distelli.objectStore.ObjectStore;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import okhttp3.ConnectionPool;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.Optional;

/**
 * Pipeline component that copies from one repository to another.
 */
@Log4j
@Data
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
    private RegistryBlobDb _blobDb;
    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private Provider<GcrClient.Builder> _gcrClientBuilderProvider;
    @Inject
    protected RegistryCredsDb _registryCredsDb = null;
    @Inject
    private Provider<ObjectKeyFactory> _objectKeyFactoryProvider;
    @Inject
    private Provider<ObjectStore> _objectStoreProvider;
    @Inject
    private ConnectionPool _connectionPool;

    @Override
    public Optional<PromotedImage> execute(PromotedImage promotedImage) throws Exception {
        ContainerRepo srcRepo = promotedImage.getRepo();
        String srcTag = promotedImage.getTag();
        String manifestDigestSha = promotedImage.getManifestDigestSha();
        if ( null == _repoDb || null == _manifestDb ) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }
        if ( null == srcRepo ) {
            throw new IllegalStateException("ContainerRepo must not be null");
        }
        if ( null == srcTag ) {
            throw new IllegalStateException("Tag must not be null");
        }
        // Not configured? Ignore...
        if ( null == destinationContainerRepoId ||
             null == destinationContainerRepoDomain )
        {
            log.error("PipelineComponentId="+getId()+" has null destinationContainerRepoId or destinationContainerRepoDomain");
            return (Optional.of(promotedImage));
        }
        // From the same repo? Ignore...
        if ( destinationContainerRepoId.equals(srcRepo.getId()) ) {
            log.error("PipelineComponentId="+getId()+" pushes to itself!?");
            return (Optional.of(new PromotedImage(srcRepo, srcTag, manifestDigestSha)));
        }
        // TODO: if manifestDigestSha is null, we should issue a "DELETE"
        if ( null == manifestDigestSha ) {
            log.debug("Tag delete is not implemented");
            return (Optional.of(promotedImage));
        }
        String reference = (null == tag) ? srcTag : tag;
        ContainerRepo destRepo = _repoDb.getRepo(destinationContainerRepoDomain, destinationContainerRepoId);
        // To repo that doesn't exist...
        if ( null == destRepo ) {
            // This will happen when a repo referenced by a
            // pipeline is deleted, so debug log level:
            log.debug("PipelineComponentId="+getId()+" repo does not exist domain="+
                      destinationContainerRepoDomain+" id="+
                      destinationContainerRepoId);
            return (Optional.of(promotedImage));
        }
        if ( srcRepo.isLocal() && destRepo.isLocal() ) {
            // Optimization, simply update the DB:
            RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(
                srcRepo.getDomain(),
                srcRepo.getId(),
                manifestDigestSha);
            if ( null == manifest ) {
                log.error("PipelineComponentId="+getId()+" missing manifest for domain="+srcRepo.getDomain()+
                          " repoId="+srcRepo.getId()+" tag="+manifestDigestSha);
                return (Optional.of(promotedImage));
            }
            RegistryManifest copy = manifest.toBuilder()
                .domain(destRepo.getDomain())
                .containerRepoId(destRepo.getId())
                .tag(reference)
                .build();
            _manifestDb.put(copy);
        } else {
            boolean crossRepositoryBlobMount =
                ( srcRepo.getProvider() == destRepo.getProvider() &&
                  srcRepo.getCredId().equals(destRepo.getCredId()) );

            Registry srcRegistry = Registry.createRegistry(srcRepo, false, null);
            Registry dstRegistry = Registry.createRegistry(destRepo, true, crossRepositoryBlobMount ? srcRepo.getName() : null);
            if ( null == dstRegistry || null == srcRegistry ) {
                return (Optional.of(promotedImage));
            }
            GcrManifest manifest = srcRegistry.getManifest(srcRepo.getName(), manifestDigestSha);
            if ( null == manifest ) {
                log.error("Manifest not found for repo="+srcRepo.getName()+" ref="+manifestDigestSha);
                return (Optional.of(promotedImage));
            }

            genericCopy(manifest, srcRegistry, srcRepo.getName(), srcTag, crossRepositoryBlobMount, dstRegistry, destRepo.getName(), reference);
        }
        return (Optional.of(new PromotedImage(destRepo, reference, manifestDigestSha)));
    }

    private void genericCopy(
        GcrManifest srcManifest,
        Registry srcRegistry,
        String srcRepo,
        String srcTag,
        boolean crossRepositoryBlobMount,
        Registry dstRegistry,
        String dstRepo,
        String dstTag)
        throws IOException
    {
        // Upload the referenced digests:
        for ( String digest : srcManifest.getReferencedDigests() ) {
            GcrBlobUpload upload = dstRegistry.createBlobUpload(
                dstRepo,
                digest,
                ( crossRepositoryBlobMount ) ? srcRepo : null);
            if ( upload.isComplete() ) continue;
            // TODO: Get the media type of the reference digests:
            // upload.setMediaType();
            srcRegistry.getBlob(srcRepo, digest, (in, meta) ->
                                dstRegistry.blobUploadChunk(upload, in, meta.getLength(), digest));
        }
        // Upload the manifest:
        dstRegistry.putManifest(dstRepo, dstTag, srcManifest);
    }

    @Override
    public void validate(String key) {
        if ( null == destinationContainerRepoDomain ) {
            throw new AjaxClientException(
                "Missing Param '"+key+".destinationContainerRepoDomain' in request",
                JsonError.Codes.MissingParam,
                400);
        }
        if ( null == destinationContainerRepoId) {
            throw new AjaxClientException(
                "Missing Param '"+key+".destinationContainerRepoId' in request",
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
