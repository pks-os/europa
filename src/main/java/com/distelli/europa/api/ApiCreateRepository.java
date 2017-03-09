/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.api.models.ApiCreateRepoRequest;
import com.distelli.europa.api.models.ApiRepo;
import com.distelli.europa.api.transformers.ApiRepoTransformer;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.helpers.RepositoryCreator;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiCreateRepository extends ApiBase
{
    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private RepositoryCreator _repoCreator;
    @Inject
    private ApiRepoTransformer _repoTransformer;

    public ApiCreateRepository()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext) {
        String domain = requestContext.getOwnerDomain();
        ApiCreateRepoRequest request = getContent(ApiCreateRepoRequest.class, requestContext, true);
        ContainerRepo repo = null;
        if(request.isLocal())
            repo = _repoCreator.createLocalRepo(domain,
                                                request.getRepoName());
        else
            repo = _repoCreator.createRemoteRepo(domain,
                                                 request.getRepoName(),
                                                 request.getRegistryId());
        _repoDb.save(repo);
        ApiRepo apiRepo = _repoTransformer.transform(repo);
        return toJson(apiRepo);
    }
}
