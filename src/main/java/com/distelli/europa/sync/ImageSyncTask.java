package com.distelli.europa.sync;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RawTaskEntry;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.registry.ContainerRepoNotFoundException;
import com.distelli.europa.registry.CopyImageBetweenRepos;
import com.distelli.europa.registry.ManifestNotFoundException;
import com.distelli.europa.tasks.Task;
import com.distelli.europa.tasks.TaskFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Provides a scheduled task to sync an image from one repository to another.
 */
@Data
@Builder
@Log4j
@NoArgsConstructor
@AllArgsConstructor
public class ImageSyncTask implements Task {
    @NonNull
    private String domain;
    /**
     * The {@code id} for the source container repo.
     *
     * Must not be {@code null}.
     */
    @NonNull
    private String sourceRepoId;
    /**
     * The {@code id} for the destination container repo.
     *
     * Must not be {@code null}, and must point to a local repository.
     */
    @NonNull
    private String destinationRepoId;
    /**
     * The tags to save or delete.
     */
    @NonNull
    @Singular
    private List<String> imageTags;
    /**
     * The manifest digest SHA to sync.
     *
     * If {@code null}, delete the {@code imageTags} from the destination repo.
     * Otherwise, save them to the destination repo, pointed at this value.
     */
    private String manifestDigestSha;

    public static final ObjectMapper OM = new ObjectMapper();
    public static final String ENTITY_TYPE = "sync:image";

    @Override
    public RawTaskEntry toRawTaskEntry() {
        try {
            return RawTaskEntry.builder()
                .entityType(ENTITY_TYPE)
                .entityId(destinationRepoId)
                .lockIds(Collections.singleton(getLockId()))
                .privateTaskState(OM.writeValueAsBytes(this))
                .build();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the lock ID for the task.
     */
    @JsonIgnore
    public String getLockId() {
        return String.format("%s/%s", domain, destinationRepoId);
    }

    public class Run implements Runnable {
        @Inject
        private ContainerRepoDb _repoDb;
        @Inject
        private RegistryManifestDb _manifestDb;
        @Inject
        private CopyImageBetweenRepos.Builder _copyImageBetweenReposBuilder;

        @Override
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Starting repo sync for %s", getLockId()));
            }
            try {
                if (manifestDigestSha == null) {
                    remove();
                } else {
                    add();
                }
            } catch (RuntimeException e) {
                log.error(String.format("Failed repo sync for %s", getLockId()), e);
                throw e;
            } catch (Exception e) {
                log.error(String.format("Failed repo sync for %s", getLockId()), e);
                throw new RuntimeException(e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Finished repo sync for %s", getLockId()));
            }
        }

        private void remove() {
            for (String tag : imageTags) {
                _manifestDb.remove(domain, destinationRepoId, tag);
            }
        }

        private void add() throws ManifestNotFoundException, IOException {
            ContainerRepo sourceRepo = getSourceRepo();
            ContainerRepo destinationRepo = getDestinationRepo();

            _copyImageBetweenReposBuilder
                .sourceRepo(sourceRepo)
                .destinationRepo(destinationRepo)
                .sourceReference(manifestDigestSha)
                .destinationTags(imageTags)
                .build()
                .run();
        }

        private ContainerRepo getSourceRepo() {
            if (sourceRepoId == null) {
                throw new IllegalArgumentException("Source repo id cannot be null");
            }
            ContainerRepo sourceRepo = _repoDb.getRepo(domain, sourceRepoId);
            if (sourceRepo == null) {
                throw new IllegalStateException("Failed to find source container repo",
                                                new ContainerRepoNotFoundException(domain, null, sourceRepoId));
            }
            return sourceRepo;
        }

        private ContainerRepo getDestinationRepo() {
            if (destinationRepoId == null) {
                throw new IllegalArgumentException("Destination repo id cannot be null");
            }
            ContainerRepo destinationRepo = _repoDb.getRepo(domain, destinationRepoId);
            if (destinationRepo == null) {
                throw new IllegalStateException("Failed to find destination container repo",
                                                new ContainerRepoNotFoundException(domain, null, destinationRepoId));
            }
            if (destinationRepo.getProvider() != RegistryProvider.EUROPA || !destinationRepo.isLocal()) {
                throw new IllegalArgumentException(String.format("Can only sync to a local repository, destination repo id %s has provider %s",
                                                                 destinationRepoId,
                                                                 destinationRepo.getProvider().toString()));
            }
            return destinationRepo;
        }
    }

    public static class Factory implements TaskFactory {
        @Inject
        private Injector _injector;
        public ImageSyncTask toTask(RawTaskEntry entry) {
            try {
                return OM.readValue(entry.getPrivateTaskState(), ImageSyncTask.class);
            } catch ( RuntimeException ex ) {
                throw ex;
            } catch ( Exception ex ) {
                throw new RuntimeException(ex);
            }
        }
        @Override
        public Runnable toRunnable(RawTaskEntry entry) {
            Run run = toTask(entry).new Run();
            _injector.injectMembers(run);
            return run;
        }
    }
}
