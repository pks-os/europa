/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.distelli.europa.Constants;
import org.eclipse.jetty.http.HttpMethod;
import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.ajax.AjaxErrors;
import com.distelli.europa.clients.*;
import com.distelli.europa.db.*;
import com.distelli.europa.models.*;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryCred;
import com.distelli.europa.util.*;
import com.distelli.gcr.*;
import com.distelli.gcr.GcrClient;
import com.distelli.gcr.auth.*;
import com.distelli.gcr.models.*;
import com.distelli.persistence.PageIterator;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.*;

import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class RepositoryCreator
{
    @Inject
    private Provider<GcrClient.Builder> _gcrClientBuilderProvider;
    @Inject
    private Provider<DockerHubClient.Builder> _dhClientBuilderProvider;
    @Inject
    private RegistryCredsDb _credsDb;
    @Inject
    private ContainerRepoDb _reposDb;

    public RepositoryCreator()
    {

    }

    public ContainerRepo createRemoteRepo(String domain,
                                          String repoName,
                                          String registryCredId)
    {
        //Now get the cred from the credId
        RegistryCred cred = _credsDb.getCred(domain, registryCredId);
        if(cred == null)
            throw(new AjaxClientException("Invalid Registry "+registryCredId, JsonError.Codes.BadContent, 400));
        ContainerRepo repo = new ContainerRepo();
        repo.setCredId(registryCredId);
        repo.setName(repoName);
        repo.setProvider(cred.getProvider());
        repo.setRegion(cred.getRegion());
        repo.setId(CompactUUID.randomUUID().toString());
        repo.setOverviewId(CompactUUID.randomUUID().toString());
        repo.setLocal(false);
        repo.setPublicRepo(false);
        repo.setDomain(domain);
        validateContainerRepo(repo, cred);
        //before we save the repo in the db lets check that the repo
        //doesn't already exist
        if(_reposDb.repoExists(domain,
                               repo.getProvider(),
                               repo.getRegion(),
                               repo.getName()))
            throw(new AjaxClientException("The specified container repo is already connected: "+
                                          repo.getProvider()+", "+repo.getName(),
                                          AjaxErrors.Codes.RepoAlreadyConnected,
                                          400));
        return repo;
    }

    public ContainerRepo createLocalRepo(String domain, String repoName)
    {
        String region = "";
        ContainerRepo repo = _reposDb.getRepo(domain,
                                              RegistryProvider.EUROPA,
                                              region,
                                              repoName);
        if(repo != null)
            throw(new AjaxClientException("The specified Repository already exists",
                                          AjaxErrors.Codes.RepoAlreadyExists,
                                          400));
        Matcher m = Constants.REPO_NAME_PATTERN.matcher(repoName);
        if(!m.matches())
            throw(new AjaxClientException("The Repo Name is invalid. It must match regex [a-zA-Z0-9_.-]",
                                          AjaxErrors.Codes.BadRepoName,
                                          400));
        repo = ContainerRepo.builder()
            .domain(domain)
            .name(repoName)
            .region("")
            .provider(RegistryProvider.EUROPA)
            .local(true)
            .publicRepo(false)
            .build();

        repo.setOverviewId(CompactUUID.randomUUID().toString());
        repo.setId(CompactUUID.randomUUID().toString());
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
            .gcrRegion(GcrRegion.getRegion(cred.getRegion()))
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
