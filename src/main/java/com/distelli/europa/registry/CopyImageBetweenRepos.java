package com.distelli.europa.registry;

import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Registry;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.util.Tag;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import lombok.EqualsAndHashCode;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Copy an image between two repositories.
 *
 * Use this class by injecting the builder then calling {@link #run()}; for example:
 *
 * <p>
 * <pre><code>
 *{@literal @}Inject
 * private CopyImageBetweenRepos.Builder _copyImageBetweenReposBuilder;
 *
 * public void fooMethod() {
 *     _copyImageBetweenReposBuilder
 *         .sourceRepo(fooRepo)
 *         .destinationRepo(barRepo)
 *         .sourceReference(fooDigest)
 *         .destinationTag("latest")
 *         .build()
 *         .run()
 * }
 * </code></pre>
 */
@EqualsAndHashCode
public final class CopyImageBetweenRepos {
    private final ContainerRepo sourceRepo;
    private final ContainerRepo destinationRepo;
    private final String sourceReference;
    private final Set<String> destinationTags;
    private RegistryManifestDb _manifestDb;
    private RegistryFactory _registryFactory;

    /**
     * Perform the copy operation.
     *
     * @throws ManifestNotFoundException thrown if we cannot load the desired
     *                                   manifest from the source registry
     * @throws IOException thrown if we have any issues reading or writing an
     *                     object for a registry
     */
    public void run() throws ManifestNotFoundException, IOException {
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

    private CopyImageBetweenRepos(Builder builder) {
        if (null == builder._manifestDb || null == builder._registryFactory) {
            throw new IllegalStateException("Injector.injectMembers(this) has not been called");
        }
        if (null == builder.sourceRepo) {
            throw new IllegalArgumentException("Source repository must not be null");
        }
        if (null == builder.destinationRepo) {
            throw new IllegalArgumentException("Destination repository must not be null");
        }
        if (null == builder.sourceReference) {
            throw new IllegalArgumentException("Source reference must not be null");
        }
        if (!Tag.isValid(builder.sourceReference)) {
            throw new IllegalArgumentException("Source reference must be a valid tag or digest");
        }
        if (!builder.destinationTags.isEmpty()) {
            for (String tag : builder.destinationTags) {
                if (!Tag.isValid(tag)) {
                    throw new IllegalArgumentException(String.format("Destination tag must be a valid tag or digest, got invalid value %s", tag));
                }
            }
        }
        this.sourceRepo = builder.sourceRepo;
        this.destinationRepo = builder.destinationRepo;
        this.sourceReference = builder.sourceReference;
        this.destinationTags = (builder.destinationTags.isEmpty())
            ? Collections.unmodifiableSet(new HashSet<>(Collections.singleton(builder.sourceReference)))
            : Collections.unmodifiableSet(builder.destinationTags);
        this._manifestDb = builder._manifestDb;
        this._registryFactory = builder._registryFactory;
    }

    /**
     * Use this via dependency injection:
     *
     * <pre><code>
     *{@literal @}Inject
     * private CopyImageBetweenRepos.Builder _copyImageBetweenReposBuilder;
     * </code></pre>
     */
    public static class Builder {

        private ContainerRepo sourceRepo;
        private ContainerRepo destinationRepo;
        private String sourceReference;
        private Set<String> destinationTags = new HashSet<>();

        @Inject
        private RegistryManifestDb _manifestDb;
        @Inject
        private RegistryFactory _registryFactory;

        /**
         * Set the source repo to copy from.
         */
        public Builder sourceRepo(ContainerRepo sourceRepo) {
            this.sourceRepo = sourceRepo;
            return this;
        }

        /**
         * Set the destination repo to copy to.
         */
        public Builder destinationRepo(ContainerRepo destinationRepo) {
            this.destinationRepo = destinationRepo;
            return this;
        }

        /**
         * Set the tag or manifest digest SHA for the source image.
         */
        public Builder sourceReference(String sourceReference) {
            this.sourceReference = sourceReference;
            return this;
        }

        /**
         * Add a tag to use for the destination image.
         *
         * If none are set, it will use the value set with {@link #sourceReference(String)}
         */
        public Builder destinationTag(String destinationTag) {
            this.destinationTags.add(destinationTag);
            return this;
        }

        /**
         * Add multiple tags to use for the destination image.
         *
         * If none are set, it will use the value set with {@link #sourceReference(String)}
         */
        public Builder destinationTags(Collection<String> destinationTags) {
            this.destinationTags.addAll(destinationTags);
            return this;
        }

        public CopyImageBetweenRepos build() {
            return new CopyImageBetweenRepos(this);
        }
    }
}
