package com.distelli.europa.models;

import com.distelli.gcr.models.GcrBlobMeta;
import com.distelli.gcr.models.GcrBlobReader;
import com.distelli.gcr.models.GcrBlobUpload;
import com.distelli.gcr.models.GcrManifest;
import com.distelli.gcr.models.GcrManifestMeta;

import java.io.IOException;
import java.io.InputStream;

/**
 * A remote or local registry that we can interact with.
 */
public interface Registry {
    /**
     * Retrieve a particular manifest from the remote
     *
     * @param repository the name of the repository
     * @param reference the tag or SHA for the manifest
     * @return GcrManifest object corresponding to the manifest data retrieved from the remote
     * @throws IOException exception on failure to connect to remote
     */
    GcrManifest getManifest(String repository, String reference) throws IOException;

    /**
     * Retrieve blob data from the remote
     *
     * Typical usage might be to provide a lambda expression for {@code reader}
     * which calls {@link #blobUploadChunk(GcrBlobUpload, InputStream, Long, String)}
     * on another Registry to run the data elsewhere.
     *
     * @param repository the name of the repository
     * @param digest the SHA digest of the blob
     * @param reader the reader which acts on the blob data
     * @throws IOException exception on failure to connect to remote
     */
    <T> T getBlob(String repository, String digest, GcrBlobReader<T> reader) throws IOException;

    /**
     * Initialize a new blob upload
     *
     * @param repository the name of the destination repository
     * @param digest the SHA digest of the blob
     * @param fromRepository the name of the source repository, if from the same repository
     * @return the
     * @throws IOException exception on failure to connect to remote
     */
    GcrBlobUpload createBlobUpload(String repository, String digest, String fromRepository) throws IOException;

    /**
     * Upload a blob chunk to a previously-initialized blob upload
     *
     * @param blobUpload the previously-initialized blob upload
     * @param chunk the data to send
     * @param chunkLength the length of the data to upload
     * @param digest the SHA digest of the blob
     * @return metadata about the uploaded blob
     * @throws IOException exception on failure to connect to remote
     */
    GcrBlobMeta blobUploadChunk(GcrBlobUpload blobUpload, InputStream chunk, Long chunkLength, String digest) throws IOException;

    /**
     * Upload an image manifest
     *
     * @param repository the name of the repository
     * @param reference the tag or SHA digest for the manifest
     * @param manifest the actual manifest to upload
     * @return metadata about the uploaded manifest
     * @throws IOException exception on failure to connect to remote
     */
    GcrManifestMeta putManifest(String repository, String reference, GcrManifest manifest) throws IOException;

    /**
     * Get a Registry object which can connect to the registry for the specified {@link ContainerRepo}
     *
     * @param repo the repository we want to connect to
     * @param isPush {@code true} if we need to push to the repository, {@code false} if we don't
     * @param crossBlobMountFrom the name of the source repository, if we're pushing to this registry
     * @return a Registry object for the requested registry
     * @throws IOException exception on failure to connect to remote
     */
    static Registry createRegistry(ContainerRepo repo, boolean isPush, String crossBlobMountFrom) throws IOException {
        switch (repo.getProvider()) {
            case DOCKERHUB:
                return new DockerHubRegistry(repo, isPush, crossBlobMountFrom);
            case GCR:
                return new GcrRegistry(repo);
            case ECR:
                return new EcrRegistry(repo);
            case EUROPA:
                return new EuropaRegistry(repo);
            default:
                throw new UnsupportedOperationException(String.format("No Registry implementation for provider=%s",
                                                                      repo.getProvider()));
        }
    }
}
