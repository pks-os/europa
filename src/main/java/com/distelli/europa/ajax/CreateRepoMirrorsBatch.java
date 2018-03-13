package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.europa.db.TasksDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Monitor;
import com.distelli.europa.models.RegistryCred;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.sync.RepoSyncTask;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.distelli.webserver.JsonError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CreateRepoMirrorsBatch extends AjaxHelper<EuropaRequestContext> {
    private static ObjectMapper OM = new ObjectMapper();

    @Inject
    private RegistryCredsDb _credsDb;
    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private TasksDb _tasksDb;
    @Inject
    private Provider<Monitor> _monitorProvider;
    @Inject
    private PermissionCheck _permissionCheck;

    public CreateRepoMirrorsBatch() {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    @Override
    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext) {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);

        String domain = requestContext.getOwnerDomain();
        String credId = ajaxRequest.getParam("credId", true);
        RegistryCred cred = _credsDb.getCred(domain, credId);
        if (null == cred) {
            throw(new AjaxClientException(String.format("Invalid Registry Cred: %s", credId),
                                          JsonError.Codes.BadParam,
                                          400));
        }

        JsonNode repos = ajaxRequest.getContent("/repos", true);
        List<NewMirrorRequest> mirrorRequests = OM.convertValue(repos, new TypeReference<List<NewMirrorRequest>>(){});

        List<String> invalidRepoNames = mirrorRequests.stream()
            .filter(request -> !ContainerRepo.isValidName(request.getDestinationRepoName()))
            .map(NewMirrorRequest::getDestinationRepoName)
            .collect(Collectors.toList());
        if (!invalidRepoNames.isEmpty()) {
            String message = String.format("The following repository names are invalid: %s\nRepository names must match the regex [a-zA-Z0-9_.-]+",
                                          String.join(", ", invalidRepoNames));
            throw(new AjaxClientException(message, AjaxErrors.Codes.BadRepoName, 400));
        }

        List<String> existingRepoNames = mirrorRequests.stream()
            .filter(request -> null != _repoDb.getRepo(domain,
                                                       RegistryProvider.EUROPA,
                                                       "",
                                                       request.getDestinationRepoName()))
            .map(NewMirrorRequest::getDestinationRepoName)
            .collect(Collectors.toList());

        if (!existingRepoNames.isEmpty()) {
            String message = String.format("The following repositories already exist: %s",
                                           String.join(", ", existingRepoNames));
            throw(new AjaxClientException(message, AjaxErrors.Codes.RepoAlreadyExists, 400));
        }

        List<ContainerRepo> containerRepos = new ArrayList<>();
        for (NewMirrorRequest request : mirrorRequests) {
            ContainerRepo destinationRepo = newMirrorRepo(domain, request);
            _repoDb.save(destinationRepo);
            ContainerRepo sourceRepo = _repoDb.getRepo(domain,
                                                       cred.getProvider(),
                                                       cred.getRegion(),
                                                       request.getSourceRepoName());
            if (null == sourceRepo) {
                sourceRepo = newSourceRepo(domain, request, cred, destinationRepo.getId());
                _repoDb.save(sourceRepo);
            } else {
                sourceRepo.getSyncDestinationContainerRepoIds().add(destinationRepo.getId());
                _repoDb.addSyncDestinationContainerRepoId(domain, sourceRepo.getId(), destinationRepo.getId());
            }
            containerRepos.add(sourceRepo);
            containerRepos.add(destinationRepo);
            _tasksDb.addTask(_monitorProvider.get(),
                             RepoSyncTask.builder()
                                 .domain(domain)
                                 .sourceRepoId(sourceRepo.getId())
                                 .destinationRepoId(destinationRepo.getId())
                                 .build());
        }

        return containerRepos;
    }

    private ContainerRepo newMirrorRepo(String domain, NewMirrorRequest request) {
        return ContainerRepo.builder()
            .domain(domain)
            .name(request.getDestinationRepoName())
            .provider(RegistryProvider.EUROPA)
            .region("")
            .local(true)
            .publicRepo(false)
            .mirror(true)
            .id(CompactUUID.randomUUID().toString())
            .overviewId(CompactUUID.randomUUID().toString())
            .build();
    }

    private ContainerRepo newSourceRepo(String domain,
                                        NewMirrorRequest request,
                                        RegistryCred cred,
                                        String destinationRepoId) {
        return ContainerRepo.builder()
            .domain(domain)
            .name(request.getSourceRepoName())
            .provider(cred.getProvider())
            .region(cred.getRegion())
            .local(false)
            .publicRepo(false)
            .mirror(false)
            .credId(cred.getId())
            .id(CompactUUID.randomUUID().toString())
            .overviewId(CompactUUID.randomUUID().toString())
            .syncDestinationContainerRepoIds(new HashSet<>(Collections.singletonList(destinationRepoId)))
            .build();
    }

    @Data
    public static class NewMirrorRequest {
        private String sourceRepoName;
        private String destinationRepoName;
    }
}
