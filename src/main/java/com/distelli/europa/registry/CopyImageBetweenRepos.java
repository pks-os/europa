package com.distelli.europa.registry;

import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Registry;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.util.Tag;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Copy an image between two repositories
 *
 * This class supports chaining; for example,
 * <p>
 *     {@code new CopyImageBetweenRepos()
 *                .sourceRepo(fooRepo)
 *                .destinationRepo(barRepo)
 *                .sourceReference(fooDigest)
 *                .destinationTag("latest")
 *                .run()}
 */
@NoArgsConstructor
public class CopyImageBetweenRepos {
    private ContainerRepo sourceRepo;
    private ContainerRepo destinationRepo;
    private String sourceReference;
    private String destinationTag;
    private boolean hasRun = false;

    @Inject
    private RegistryManifestDb _manifestDb;

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
     * Set the tag to use for the remote image.
     *
     * Defaults to the value set with {@link #sourceReference(String)}.
     */
    public CopyImageBetweenRepos destinationTag(String destinationTag) {
        this.destinationTag = destinationTag;
        return this;
    }

    /**
     * Perform the copy operation.
     *
     * @throws RegistryNotFoundException thrown if we cannot connect to either
     *                                   the source or destination registry
     * @throws ManifestNotFoundException thrown if we cannot load the desired
     *                                   manifest from the source registry
     * @throws IOException thrown if we have any issues reading or writing an
     *                     object for a registry
     * @throws DuplicateRegistryOperationException thrown if called twice
     */
    public void run() throws RegistryNotFoundException, ManifestNotFoundException, IOException, DuplicateRegistryOperationException {
        validate();
        hasRun = true;
        destinationTag = (destinationTag == null) ? sourceReference : destinationTag;
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
        RegistryManifest copy = manifest.toBuilder()
            .domain(destinationRepo.getDomain())
            .containerRepoId(destinationRepo.getId())
            .tag(destinationTag)
            .build();
        _manifestDb.put(copy);
    }

    private void copyRemote() throws RegistryNotFoundException, ManifestNotFoundException, IOException {
        boolean crossRepositoryBlobMount = (sourceRepo.getProvider() == destinationRepo.getProvider() &&
                                            sourceRepo.getCredId().equalsIgnoreCase(destinationRepo.getCredId()));
        String crossBlobMountFrom = (crossRepositoryBlobMount) ? sourceRepo.getName() : null;

        Registry sourceRegistry;
        Registry destinationRegistry;
        GcrManifest manifest;

        try {
            sourceRegistry = Registry.createRegistry(sourceRepo, false, null);
        } catch (IOException e) {
            throw new RegistryNotFoundException(sourceRepo.getProvider(), sourceRepo.getName(), e);
        }
        if (sourceRegistry == null) {
            throw new RegistryNotFoundException(sourceRepo.getProvider(), sourceRepo.getName());
        }

        try {
            destinationRegistry = Registry.createRegistry(destinationRepo, true, crossBlobMountFrom);
        } catch (IOException e) {
            throw new RegistryNotFoundException(destinationRepo.getProvider(), destinationRepo.getName(), e);
        }
        if (destinationRegistry == null) {
            throw new RegistryNotFoundException(destinationRepo.getProvider(), destinationRepo.getName());
        }

        try {
            manifest = sourceRegistry.getManifest(sourceRepo.getName(), sourceReference);
        } catch (IOException e) {
            throw new ManifestNotFoundException(sourceRepo.getName(), sourceReference, e);
        }
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

        destinationRegistry.putManifest(destinationRepo.getName(), destinationTag, manifest);
    }

    private void validate() throws DuplicateRegistryOperationException {
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
        if (destinationTag != null && !Tag.isValid(destinationTag)) {
            throw new IllegalArgumentException("Destination tag must be a valid tag or digest");
        }
    }
}
