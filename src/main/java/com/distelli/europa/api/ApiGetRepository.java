/*
  $Id: $
  @file ApiGetRepository.java
  @brief Contains the ApiGetRepository.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
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
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiGetRepository extends ApiBase
{
    @Inject
    private ContainerRepoDb _db;
    @Inject
    private ApiRepoTransformer _repoTransformer;

    public ApiGetRepository()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext) {
        String repoId = getPathParam("id", requestContext);
        String domain = requestContext.getOwnerDomain();

        ContainerRepo repo = _db.getRepoById(domain, repoId);
        if(repo == null)
            throw(new AjaxClientException("Repo "+repoId+" not found",
                                          AjaxErrors.Codes.RepoNotFound, 404));
        ApiRepo apiRepo = _repoTransformer.transform(repo);
        return toJson(apiRepo);
    }
}
