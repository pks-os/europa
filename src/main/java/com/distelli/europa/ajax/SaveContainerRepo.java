/*
  $Id: $
  @file SaveContainerRepo.java
  @brief Contains the SaveContainerRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.clients.DockerHubClient;
import com.distelli.europa.clients.ECRClient;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.NotificationsDb;
import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.Notification;
import com.distelli.europa.models.RegistryCred;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.util.FieldValidator;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.GcrIterator;
import com.distelli.gcr.GcrRegion;
import com.distelli.gcr.auth.GcrServiceAccountCredentials;
import com.distelli.gcr.models.GcrImageTag;
import com.distelli.persistence.PageIterator;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.distelli.webserver.JsonError;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Log4j
@Singleton
public class SaveContainerRepo extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    private RegistryCredsDb _credsDb;
    @Inject
    private ContainerRepoDb _reposDb;
    @Inject
    private NotificationsDb _notificationDb;
    @Inject
    private Provider<GcrClient.Builder> _gcrClientBuilderProvider;
    @Inject
    private Provider<DockerHubClient.Builder> _dhClientBuilderProvider;
    @Inject
    protected PermissionCheck _permissionCheck;

    public SaveContainerRepo()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);
        ContainerRepo repo = ajaxRequest.convertContent("/repo", ContainerRepo.class,
                                                       true); //throw if null
        //Validate that the fields we want are non-null
        FieldValidator.validateNonNull(repo, "credId", "name");
        String repoDomain = requestContext.getOwnerDomain();
        //Now get the cred from the credId
        RegistryCred cred = _credsDb.getCred(repoDomain, repo.getCredId());
        if(cred == null)
            throw(new AjaxClientException("Invalid Registry Cred: "+repo.getCredId(), JsonError.Codes.BadContent, 400));
        repo.setProvider(cred.getProvider());
        repo.setRegion(cred.getRegion());
        repo.setId(CompactUUID.randomUUID().toString());
        repo.setOverviewId(CompactUUID.randomUUID().toString());
        repo.setLocal(false);
        repo.setPublicRepo(false);
        repo.setDomain(repoDomain);
        validateContainerRepo(repo, cred);
        //before we save the repo in the db lets check that the repo
        //doesn't already exist
        if(_reposDb.repoExists(repoDomain, repo.getProvider(), repo.getRegion(), repo.getName()))
            throw(new AjaxClientException("The specified container repo is already connected: "+
                                          repo.getProvider()+", "+repo.getName(),
                                          AjaxErrors.Codes.RepoAlreadyConnected,
                                          400));
        Notification notification = ajaxRequest.convertContent("/notification", Notification.class, false);
        if(notification != null) {
            FieldValidator.validateNonNull(notification, "type", "target");

            try {
                URL url = new URL(notification.getTarget());
            } catch(MalformedURLException mue) {
                throw(new AjaxClientException("Invalid Target URL on Webhook Notification: "+notification.getTarget(),
                                              JsonError.Codes.BadContent, 400));
            }

            notification.setRepoId(repo.getId());
            notification.setDomain(repoDomain);
            notification.setRepoProvider(repo.getProvider());
            notification.setRegion(repo.getRegion());
            notification.setRepoName(repo.getName());
        }
        //save the repo in the db
        _reposDb.save(repo);

        if(notification != null)
            _notificationDb.save(notification);

        return repo;
    }

    /**
       Validates the repo. if its an ecr repo it sets the registry Id
       as well after validation
     */
    private void validateContainerRepo(ContainerRepo repo, RegistryCred cred)
    {
        RegistryProvider provider = cred.getProvider();
        switch(provider)
        {
        case ECR:
            String registryId = validateEcrRepo(repo, cred);
            repo.setRegistryId(registryId);
            break;
        case GCR:
            validateGcrRepo(repo, cred);
            break;
        case DOCKERHUB:
            validateDockerHubRepo(repo, cred);
            break;
        default:
            throw(new AjaxClientException("Unsupported Container Registry: "+provider, JsonError.Codes.BadContent, 400));
        }
    }

    private void validateDockerHubRepo(ContainerRepo repo, RegistryCred cred)
    {
        DockerHubClient client = _dhClientBuilderProvider.get()
            .credentials(cred.getUsername(), cred.getPassword())
            .build();
        try {
            client.listRepoTags(repo.getName(), new PageIterator().pageSize(1));
        } catch ( Throwable ex ) {
            throw new AjaxClientException("Invalid Container Repository or Credentials: "+ex.getMessage(),
                                          JsonError.Codes.BadContent, 400);
        }
    }

    private void validateGcrRepo(ContainerRepo repo, RegistryCred cred)
    {
        GcrClient gcrClient = _gcrClientBuilderProvider.get()
            .gcrCredentials(new GcrServiceAccountCredentials(cred.getSecret()))
            .gcrRegion(GcrRegion.getRegionByEndpoint(cred.getRegion()))
            .build();

        GcrIterator iter = GcrIterator.builder().pageSize(1).build();
        try {
            List<GcrImageTag> tags = gcrClient.listImageTags(repo.getName(), iter);
        } catch(Throwable t) {
            throw(new AjaxClientException("Invalid Container Repository or Credentials: "+t.getMessage(), JsonError.Codes.BadContent, 400));
        }
   }

    //Validates the ECR repo and returns the AWS RegistryId if the
    //repo is valid
    private String validateEcrRepo(ContainerRepo repo, RegistryCred cred)
    {
        ECRClient ecrClient = new ECRClient(cred);
        ContainerRepo ecrRepo = ecrClient.getRepository(repo.getName());
        if(ecrRepo == null)
            throw(new AjaxClientException("Invalid Container Repository or Credentials", JsonError.Codes.BadContent, 400));
        return ecrRepo.getRegistryId();
    }
}
