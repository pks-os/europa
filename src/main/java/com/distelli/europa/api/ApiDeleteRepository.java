/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.ajax.AjaxErrors;
import com.distelli.europa.api.models.*;
import com.distelli.europa.api.transformers.*;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonSuccess;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiDeleteRepository extends ApiBase
{
    @Inject
    private ContainerRepoDb _db;

    public ApiDeleteRepository()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext) {
        String repoId = getPathParam("id", requestContext);
        String domain = requestContext.getOwnerDomain();

        ContainerRepo repo = _db.getRepoById(domain, repoId);
        if(repo == null)
            throw(new AjaxClientException("Repo "+repoId+" not found",
                                          AjaxErrors.Codes.RepoNotFound, 404));
        _db.deleteRepo(domain, repoId);
        return toJson(JsonSuccess.Success);
    }
}
