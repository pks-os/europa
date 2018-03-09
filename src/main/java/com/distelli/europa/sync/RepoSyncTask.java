package com.distelli.europa.sync;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.db.TasksDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Monitor;
import com.distelli.europa.models.MultiTaggedManifest;
import com.distelli.europa.models.RawTaskEntry;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.registry.ContainerRepoNotFoundException;
import com.distelli.europa.tasks.Task;
import com.distelli.europa.tasks.TaskFactory;
import com.distelli.persistence.PageIterator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a scheduled task to sync an entire repository to another.
 */
@Data
@Builder
@Log4j
@NoArgsConstructor
@AllArgsConstructor
public class RepoSyncTask implements Task {
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

    public static final ObjectMapper OM = new ObjectMapper();
    public static final String ENTITY_TYPE = "sync:repo";

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
        private TasksDb _tasksDb;
        @Inject
        private Provider<Monitor> _monitorProvider;

        @Override
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Starting scheduling repo sync for %s", getLockId()));
            }
            try {
                // verify that the source repo exists
                getSourceRepo();
                ContainerRepo destinationRepo = getDestinationRepo();

                List<MultiTaggedManifest> manifests = new ArrayList<>();
                for (PageIterator it : new PageIterator().pageSize(1000)) {
                    manifests.addAll(_manifestDb.listMultiTaggedManifest(domain, sourceRepoId, it));
                }
                for (MultiTaggedManifest manifest : manifests) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Adding sync task from repo id %s to repo id %s for image %s",
                                                sourceRepoId,
                                                destinationRepoId,
                                                manifest.getManifestId()));
                    }
                    _tasksDb.addTask(_monitorProvider.get(),
                                     ImageSyncTask.builder()
                                         .domain(domain)
                                         .sourceRepoId(sourceRepoId)
                                         .destinationRepoId(destinationRepoId)
                                         .imageTags(manifest.getTags())
                                         .manifestDigestSha(manifest.getManifestId())
                                         .build());
                    destinationRepo.setLastSyncTime(System.currentTimeMillis());
                }
            } catch (RuntimeException e) {
                log.error(String.format("Failed repo sync for %s", getLockId()), e);
                throw e;
            } catch (Exception e) {
                log.error(String.format("Failed repo sync for %s", getLockId()), e);
                throw new RuntimeException(e);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Finished scheduling repo sync for %s", getLockId()));
            }
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
            if (destinationRepo.getProvider() != RegistryProvider.EUROPA ||
                !destinationRepo.isLocal() ||
                !destinationRepo.isMirror()) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("Can only sync to a local cache repository, destination repo id %s has provider %s",
                                             destinationRepoId,
                                             destinationRepo.getProvider().toString()));
                if (!destinationRepo.isLocal()) {
                    message.append(" and is not local");
                }
                if (!destinationRepo.isMirror()) {
                    message.append(" and is not a cache repo");
                }
                throw new IllegalArgumentException(message.toString());
            }
            return destinationRepo;
        }
    }

    public static class Factory implements TaskFactory {
        @Inject
        private Injector _injector;

        public RepoSyncTask toTask(RawTaskEntry entry) {
            try {
                return OM.readValue(entry.getPrivateTaskState(), RepoSyncTask.class);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
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
