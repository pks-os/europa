/*
  $Id: $
  @file WebAppRoutes.java
  @brief Contains the WebAppRoutes.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.DefaultServlet;
import com.distelli.webserver.AjaxRequestHandler;
import com.distelli.webserver.RouteMatcher;
import com.distelli.europa.handlers.*;
import com.distelli.europa.api.*;
import lombok.extern.log4j.Log4j;

@Log4j
public class WebAppRoutes
{
    private static final RouteMatcher ROUTES = new RouteMatcher();

    public static RouteMatcher getRouteMatcher() {
        return ROUTES;
    }

    static {
        //Add the routes below this line
        //Ajax Routes
        ROUTES.add("GET", "/ajax", AjaxRequestHandler.class);
        ROUTES.add("POST", "/ajax", AjaxRequestHandler.class);
        ROUTES.add("POST", "/storage", AjaxRequestHandler.class);

        //API Routes
        ROUTES.add("GET", "/api/v1/repositories", ApiListRepositories.class);
        ROUTES.add("GET", "/api/v1/repositories/:id", ApiGetRepository.class);
        // ROUTES.add("GET", "/api/v1/repositories/:id/tags", ApiListRepositoryTags.class);
        // ROUTES.add("GET", "/api/v1/repositories/:id/manifests", ApiListRepositoryManifests.class);
        // ROUTES.add("GET", "/api/v1/repositories/:id/events", ApiListRepositoryEvents.class);
        // ROUTES.add("DELETE", "/api/v1/repositories/:id", ApiDeleteRepository.class);
        // ROUTES.add("PUT", "/api/v1/repositories", ApiCreateRepository.class);

        // ROUTES.add("PUT", "/api/v1/credentials", ApiAddRegistryCredentials.class);
        // ROUTES.add("GET", "/api/v1/credentials", ApiListRegistryCredentials.class);
        // ROUTES.add("DELETE", "/api/v1/credentials", ApiDeleteRegistryCredentials.class);
        // ROUTES.add("GET", "/api/v1/credentials/:id", ApiGetRegistryCredentials.class);

        // ROUTES.add("GET", "/api/v1/pipelines", ApiListPipelines.class);
        // ROUTES.add("GET", "/api/v1/pipelines/:name", ApiGetPipeline.class);
        // ROUTES.add("PUT", "/api/v1/pipelines/:name/stages", ApiCreatePipelineStage.class);
        // ROUTES.add("DELETE", "/api/v1/pipelines/:name/stages/:id", ApiRemovePipelineStage.class);
        // ROUTES.add("DELETE", "/api/v1/pipelines/:name", ApiDeletePipeline.class);
        // ROUTES.add("PUT", "/api/v1/pipelines", ApiCreatePipelines.class);

        // //Groups
        // ROUTES.add("GET", "/api/v1/groups", ApiListGroups.class);
        // ROUTES.add("PUT", "/api/v1/groups", ApiCreateGroup.class);
        // ROUTES.add("DELETE", "/api/v1/groups/:name", ApiDeleteGroup.class);
        // ROUTES.add("GET", "/api/v1/groups/:name", ApiGetGroup.class);

        // ROUTES.add("PUT", "/api/v1/groups/:name/grants", ApiAddGrant.class);
        // ROUTES.add("GET", "/api/v1/groups/:name/grants", ApiListGrants.class);
        // ROUTES.add("GET", "/api/v1/groups/:name/users", ApiListUsers.class);
        // ROUTES.add("DELETE", "/api/v1/groups/:name/users/:id", ApiRemoveUser.class);

        // ROUTES.add("GET", "/api/v1/users", ApiListUsers.class);
        // ROUTES.add("GET", "/api/v1/users/:id", ApiGetUser.class);

        ROUTES.setDefaultRequestHandler(DefaultRequestHandler.class);
    }
}
