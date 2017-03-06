/*
  $Id: $
  @file ApiRepoList.java
  @brief Contains the ApiRepoList.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRepoList
{
    protected List<ApiRepo> repositories;
    protected String marker;
}
