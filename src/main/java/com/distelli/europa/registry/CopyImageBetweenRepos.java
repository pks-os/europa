package com.distelli.europa.registry;

import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Registry;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.util.Tag;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Copy an image between two repositories.
 *
 * Use this class by injecting a provider, then chaining the methods before
 * calling {@link #run()}; for example,
 *
 * <pre>
 * {@code
 * _copyImageBetweenReposProvider.get()
 *     .sourceRepo(fooRepo)
 *     .destinationRepo(barRepo)
 *     .sourceReference(fooDigest)
 *     .destinationTag("latest")
 *     .run()
 * }
 * </pre>
 */
@NoArgsConstructor
@Log4j
public final class CopyImageBetweenRepos {

    private ContainerRepo sourceRepo;
    private ContainerRepo destinationRepo;
    private String sourceReference;
    private Set<String> destinationTags = new HashSet<>();
    private boolean hasRun = false;

    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    private RegistryFactory _registryFactory;

    /**
     * Set the source repository to copy from.
     */
    public CopyImageBetweenRepos sourceRepo(ContainerRepo sourceRepo) {
        this.sourceRepo = sourceRepo;
        return this;
    }

    /**
     * Set the destination repository to copy to.
     */
    public CopyImageBetweenRepos destinationRepo(ContainerRepo destinationRepo) {
        this.destinationRepo = destinationRepo;
        return this;
    }

    /**
     * Set the tag or manifest digest SHA for the source image.
     */
    public CopyImageBetweenRepos sourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
        return this;
    }

    /**
     * Add a tag to use for the remote image.
     *
     * If none are set, it will use the value set with {@link #sourceReference(String)}.
     */
    public CopyImageBetweenRepos destinationTag(String destinationTag) {
        this.destinationTags.add(destinationTag);
        return this;
    }

    /**
     * Add multiple tags to use for the remote image.
     *
     * If none are set, to the value set with {@link #sourceReference(String)}.
     */
    public CopyImageBetweenRepos destinationTags(Collection<String> destinationTags) {
        this.destinationTags.addAll(destinationTags);
        return this;
    }

    /**
     * Perform the copy operation.
     *
     * @throws ManifestNotFoundException thrown if we cannot load the desired
     *                                   manifest from the source registry
     * @throws IOException thrown if we have any issues reading or writing an
     *                     object for a registry
     * @throws DuplicateRegistryOperationException thrown if called twice
     */
    public void run() throws ManifestNotFoundException, IOException {
        validate();
        hasRun = true;
        if (destinationTags.isEmpty()) {
            destinationTags.add(sourceReference);
        }
        if (sourceRepo.isLocal() && destinationRepo.isLocal()) {
            copyLocal();
        } else {
            copyRemote();
        }
    }

    private void copyLocal() throws ManifestNotFoundException {
        RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(sourceRepo.getDomain(),
                                                                       sourceRepo.getId(),
                                                                       sourceReference);
        if (manifest == null) {
            throw new ManifestNotFoundException(sourceRepo.getName(), sourceReference);
        }
        for (String tag : destinationTags) {
            RegistryManifest copy = manifest.toBuilder()
                .domain(destinationRepo.getDomain())
                .containerRepoId(destinationRepo.getId())
                .tag(tag)
                .build();
            _manifestDb.put(copy);
        }
    }

    private void copyRemote() throws ManifestNotFoundException, IOException {
        boolean crossRepositoryBlobMount = (sourceRepo.getProvider() == destinationRepo.getProvider() &&
                                            sourceRepo.getCredId().equalsIgnoreCase(destinationRepo.getCredId()));
        String crossBlobMountFrom = (crossRepositoryBlobMount) ? sourceRepo.getName() : null;

        Registry sourceRegistry;
        Registry destinationRegistry;
        GcrManifest manifest;

        sourceRegistry = _registryFactory.createRegistry(sourceRepo, Boolean.FALSE, null);

        destinationRegistry = _registryFactory.createRegistry(destinationRepo, Boolean.TRUE, crossBlobMountFrom);

        manifest = sourceRegistry.getManifest(sourceRepo.getName(), sourceReference);
        if (manifest == null) {
            throw new ManifestNotFoundException(sourceRepo.getName(), sourceReference);
        }

        for (String digest : manifest.getReferencedDigests()) {
            GcrBlobUpload upload = destinationRegistry.createBlobUpload(destinationRepo.getName(),
                                                                        digest,
                                                                        crossBlobMountFrom);
            if (!upload.isComplete()) {
                upload.setMediaType(manifest.getMediaType());
                sourceRegistry.getBlob(sourceRepo.getName(),
                                       digest,
                                       (in, meta) -> destinationRegistry.blobUploadChunk(upload,
                                                                                         in,
                                                                                         meta.getLength(),
                                                                                         digest));
            }
        }

        for (String tag : destinationTags) {
            destinationRegistry.putManifest(destinationRepo.getName(), tag, manifest);
        }
    }

    private void validate() {
        if (_manifestDb == null) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }
        if (hasRun) {
            throw new DuplicateRegistryOperationException(this.getClass().getSimpleName());
        }
        if (sourceRepo == null) {
            throw new IllegalArgumentException("Source repository must not be null");
        }
        if (destinationRepo == null) {
            throw new IllegalArgumentException("Destination repository must not be null");
        }
        if (sourceReference == null) {
            throw new IllegalArgumentException("Source reference must not be null");
        }
        if (!Tag.isValid(sourceReference)) {
            throw new IllegalArgumentException("Source reference must be a valid tag or digest");
        }
        if (!destinationTags.isEmpty()) {
            for (String tag : destinationTags) {
                if (!Tag.isValid(tag)) {
                    throw new IllegalArgumentException(String.format("Destination tag must be a valid tag or digest, got invalid value %s", tag));
                }
            }
        }
    }
}
