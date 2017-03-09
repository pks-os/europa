/*
  $Id: $
  @file CreateLocalRepo.java
  @brief Contains the CreateLocalRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.ajax;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.helpers.RepositoryCreator;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.distelli.webserver.JsonSuccess;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class CreateLocalRepo extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    protected ContainerRepoDb _repoDb;
    @Inject
    protected PermissionCheck _permissionCheck;
    @Inject
    protected RepositoryCreator _repoCreator;

    public CreateLocalRepo()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);

        String ownerDomain = requestContext.getOwnerDomain();
        String repoName = ajaxRequest.getParam("repoName", true);
        ContainerRepo repo = _repoCreator.createLocalRepo(ownerDomain, repoName);
        _repoDb.save(repo);
        return repo;
    }
}
