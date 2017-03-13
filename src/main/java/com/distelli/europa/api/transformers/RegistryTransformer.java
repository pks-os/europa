/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.transformers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j;

import com.distelli.europa.api.models.ApiRegistry;
import com.distelli.europa.models.RegistryCred;

@Log4j
@Singleton
public class RegistryTransformer extends TransformerBase
{
    public RegistryTransformer()
    {

    }

    public List<ApiRegistry> transform(List<RegistryCred> creds)
    {
        List<ApiRegistry> registries = new ArrayList<ApiRegistry>();
        if(creds == null || creds.size() == 0)
            return registries;
        for(RegistryCred cred : creds)
        {
            ApiRegistry registry = transform(cred);
            if(registry != null)
                registries.add(registry);
        }
        return registries;
    }

    public ApiRegistry transform(RegistryCred cred)
    {
        ApiRegistry registry = ApiRegistry
        .builder()
        .id(cred.getId())
        .createTime(toISODateTime(cred.getCreated()))
        .description(cred.getDescription())
        .provider(cred.getProvider())
        .region(cred.getRegion())
        .build();
        return registry;
    }
}
