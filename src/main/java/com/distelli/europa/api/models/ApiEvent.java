/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api.models;

import java.util.List;

import com.distelli.europa.models.RepoEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEvent
{
    protected String repoId;
    protected String id;
    protected RepoEventType eventType;
    protected String eventTime;
    protected Long imageSize;
    @Singular
    protected List<String> tags;
    protected String sha;
}
