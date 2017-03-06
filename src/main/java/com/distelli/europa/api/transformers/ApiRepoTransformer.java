/*
  $Id: $
  @file ApiRepoTransformer.java
  @brief Contains the ApiRepoTransformer.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.transformers;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.api.models.*;
import com.distelli.europa.models.ContainerRepo;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiRepoTransformer
{
    public ApiRepoTransformer()
    {

    }

    public List<ApiRepo> transform(List<ContainerRepo> repos)
    {
        List<ApiRepo> repoList = new ArrayList<ApiRepo>();
        if(repos == null || repos.size() == 0)
            return repoList;
        for(ContainerRepo repo : repos)
        {
            ApiRepo apiRepo = transform(repo);
            if(apiRepo != null)
                repoList.add(apiRepo);
        }
        return repoList;
    }

    public ApiRepo transform(ContainerRepo repo)
    {
        ApiRepo apiRepo = ApiRepo
        .builder()
        .id(repo.getId())
        .name(repo.getName())
        .region(repo.getRegion())
        .provider(repo.getProvider())
        .local(repo.isLocal())
        .isPublic(repo.isPublicRepo())
        .build();
        return apiRepo;
    }
}
