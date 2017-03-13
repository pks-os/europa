/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.models;

import com.distelli.europa.models.RegistryProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRegistry
{
    protected String id = null;
    protected String createTime = null;
    protected String description = null;
    protected RegistryProvider provider = null;
    protected String region = null;
}
