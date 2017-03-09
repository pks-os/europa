/*
  $Id: $
  @file ManifestTransformer.java
  @brief Contains the ManifestTransformer.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.transformers;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.api.models.ApiTag;
import com.distelli.europa.models.RegistryManifest;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ManifestTransformer extends TransformerBase
{
    public ManifestTransformer()
    {

    }

    public List<ApiTag> transform(List<RegistryManifest> manifests)
    {
        List<ApiTag> tags = new ArrayList<ApiTag>();
        if(manifests == null || manifests.size() == 0)
            return tags;
        for(RegistryManifest manifest : manifests)
        {
            ApiTag tag = transform(manifest);
            if(tag != null)
                tags.add(tag);
        }

        return tags;
    }

    public ApiTag transform(RegistryManifest manifest)
    {
        ApiTag tag = ApiTag
        .builder()
        .tag(manifest.getTag().toLowerCase())
        .sha(manifest.getManifestId())
        .size(manifest.getVirtualSize())
        .pushed(toISODateTime(manifest.getPushTime()))
        .build();
        return tag;
    }
}
