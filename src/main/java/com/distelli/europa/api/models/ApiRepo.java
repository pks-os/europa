/*
  $Id: $
  @file ApiRepo.java
  @brief Contains the ApiRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
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
public class ApiRepo
{
    protected String id;
    protected String name;
    protected String region;
    protected RegistryProvider provider;
    protected boolean local;
    protected boolean isPublic;
}
