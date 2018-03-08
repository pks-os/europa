/*
  $Id: $
  @file RegistryAccess.java
  @brief Contains the RegistryAccess.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.registry;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public interface RegistryAccess
{
    public void checkAccess(String operationName, EuropaRequestContext requestContext);

    @Log4j
    public static class Default implements RegistryAccess {
        @Inject
        protected ContainerRepoDb _repoDb;

        protected static final Set<String> READ_OPERATIONS = new HashSet<String>();
        protected static final Set<String> NO_REPO_OPERATIONS = new HashSet<String>();

        static {
            READ_OPERATIONS.add("RegistryLayerPull");
            READ_OPERATIONS.add("RegistryCatalog");
            READ_OPERATIONS.add("RegistryTagList");
            READ_OPERATIONS.add("RegistryManifestPull");
            READ_OPERATIONS.add("RegistryLayerExists");
            READ_OPERATIONS.add("RegistryManfestExists");

            NO_REPO_OPERATIONS.add("RegistryVersionCheck");
            NO_REPO_OPERATIONS.add("RegistryCatalog");
        }

        public void checkAccess(String operationName, EuropaRequestContext requestContext)
        {
            if(operationName.equalsIgnoreCase("RegistryDefault") ||
               operationName.equalsIgnoreCase("RegistryTokenHandler") ||
               operationName.equalsIgnoreCase("PremiumRegistryTokenHandler"))
                return;

            //Its an authenticated request with a valid token so its
            //allowed.
            if(allowAuthenticatedRequest(operationName, requestContext))
                return;
            //Registry Version check should throw an Auth Error
            if(operationName.equalsIgnoreCase("RegistryVersionCheck"))
                RequireAuthError.throwRequireAuth("Missing Authorization header", requestContext);
            checkPublicRepo(operationName, requestContext);
        }

        protected boolean allowAuthenticatedRequest(String operationName, EuropaRequestContext requestContext)
        {
            String requesterDomain = requestContext.getRequesterDomain();
            if(requesterDomain != null) {
                // Write operations are not allowed for cache repos.
                if (!isReadOperation(operationName) && !isNoRepoOperation(operationName)) {
                    String ownerDomain = requestContext.getOwnerDomain();
                    String repoName = requestContext.getMatchedRoute().getParam("name");
                    if (repoName == null) {
                        return false;
                    }
                    ContainerRepo repo = _repoDb.getLocalRepo(ownerDomain, repoName);
                    return repo != null && !repo.isCacheRepo();
                }
                return true;
            }
            return false;
        }

        protected boolean isReadOperation(String operationName)
        {
            return READ_OPERATIONS.contains(operationName);
        }

        protected boolean isNoRepoOperation(String operationName)
        {
            return NO_REPO_OPERATIONS.contains(operationName);
        }

        protected void checkPublicRepo(String operationName, EuropaRequestContext requestContext)
        {
            //Don't allow access to non-read operations
            if(!isReadOperation(operationName))
                RequireAuthError.throwRequireAuth(
                    "You do not have access to this operation", requestContext);
            String ownerDomain = requestContext.getOwnerDomain();
            String repoName = requestContext.getMatchedRoute().getParam("name");

            // NOTE: repoName is null for _catalog routes... we are erroring on the side of
            // caution and forcing authorization for this route:
            if ( null != repoName ) {
                ContainerRepo repo = _repoDb.getLocalRepo(ownerDomain,
                                                          repoName);
                //if its a public repo then allow acess
                if(repo != null && repo.isPublicRepo())
                    return;
            }

            //We've arrived here so this means that we don't allow access (or the repo was not found).
            RequireAuthError.throwRequireAuth(
                "You do not have access to this operation", requestContext);
        }
    }
}
