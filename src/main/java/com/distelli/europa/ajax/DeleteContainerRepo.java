/*
  $Id: $
  @file DeleteContainerRepo.java
  @brief Contains the DeleteContainerRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.ajax;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxRequest;
import com.distelli.webserver.HTTPMethod;
import com.distelli.webserver.JsonSuccess;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;

@Log4j
@Singleton
public class DeleteContainerRepo extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    protected ContainerRepoDb _repoDb;
    @Inject
    protected PermissionCheck _permissionCheck;

    public DeleteContainerRepo()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    /**
       Params:
       - id (required)
    */
    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        String repoId = ajaxRequest.getParam("id",
                                         true); //throw if missing
        String domain = requestContext.getOwnerDomain();
        ContainerRepo repo = _repoDb.getRepo(domain, repoId);
        if (null == repo) {
            throw (new AjaxClientException("The specified Repository was not found",
                                           AjaxErrors.Codes.RepoNotFound, 400));
        }
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext, repo);
        for (String destinationContainerRepoId : repo.getSyncDestinationContainerRepoIds()) {
            ContainerRepo destinationRepo = _repoDb.getRepo(domain, destinationContainerRepoId);
            if (null != destinationRepo) {
                throw (new AjaxClientException("Cannot disconnect source repository for a mirror. Remove the mirror first.",
                                               AjaxErrors.Codes.RepoIsMirrorSource, 400));
            }
        }
        _repoDb.deleteRepo(domain, repoId);
        return JsonSuccess.Success;
    }
}
