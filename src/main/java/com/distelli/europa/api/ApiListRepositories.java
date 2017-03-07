/*
  $Id: $
  @file ApiListRepositories.java
  @brief Contains the ApiListRepositories.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.api.models.*;
import com.distelli.europa.api.transformers.*;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiListRepositories extends ApiBase
{
    @Inject
    private ContainerRepoDb _db;
    @Inject
    private ApiRepoTransformer _repoTransformer;

    public ApiListRepositories()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext) {
        int pageSize = getParamAsInt("pageSize", 100, requestContext);
        String marker = getParam("marker", requestContext);
        String domain = requestContext.getOwnerDomain();

        PageIterator pageIterator = new PageIterator()
        .pageSize(pageSize)
        .marker(marker)
        .forward();

        List<ContainerRepo> repoList = _db.listRepos(domain, pageIterator);
        List<ApiRepo> repos = _repoTransformer.transform(repoList);
        ApiRepoList apiRepoList = ApiRepoList
        .builder()
        .repositories(repos)
        .marker(pageIterator.getMarker())
        .build();
        return toJson(apiRepoList);
    }
}
