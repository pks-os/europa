/*
  $Id: $
  @file CreateLocalRepo.java
  @brief Contains the CreateLocalRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Log4j
@Singleton
public class CreateLocalRepo extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    protected ContainerRepoDb _repoDb;
    @Inject
    protected PermissionCheck _permissionCheck;

    public CreateLocalRepo()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);

        ContainerRepo repo = getRepoToSave(ajaxRequest, requestContext);
        _repoDb.save(repo);

        return repo;
    }

    // This is used by CreateRepoMirror so we can avoid duplicate code.
    protected ContainerRepo getRepoToSave(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        String ownerDomain = requestContext.getOwnerDomain();
        String repoName = ajaxRequest.getParam("repoName", true);
        ContainerRepo repo = _repoDb.getRepo(ownerDomain, RegistryProvider.EUROPA, "", repoName);
        if(repo != null)
            throw(new AjaxClientException("The specified Repository already exists",
                                          AjaxErrors.Codes.RepoAlreadyExists,
                                          400));
        if(!ContainerRepo.isValidName(repoName))
            throw(new AjaxClientException("The Repo Name is invalid. It must match regex [a-zA-Z0-9_.-]+",
                                          AjaxErrors.Codes.BadRepoName,
                                          400));
        repo = ContainerRepo.builder()
            .domain(ownerDomain)
            .name(repoName)
            .region("")
            .provider(RegistryProvider.EUROPA)
            .local(true)
            .publicRepo(false)
            .mirror(false)
            .build();

        repo.setOverviewId(CompactUUID.randomUUID().toString());
        repo.setId(CompactUUID.randomUUID().toString());

        return repo;
    }
}
