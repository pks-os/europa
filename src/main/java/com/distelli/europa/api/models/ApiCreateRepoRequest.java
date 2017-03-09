/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCreateRepoRequest
{
    protected String repoName;
    protected String registryId;
    protected Boolean local;

    public boolean isLocal()
    {
        if(this.local == null)
            return true;
        return this.local.booleanValue();
    }
}
