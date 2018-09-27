/*
  $Id: $
  @file RegistryTokenHandler.java
  @brief Contains the RegistryTokenHandler.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.handlers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.registry.RegistryToken;
import com.distelli.europa.registry.RequireAuthError;
import com.distelli.europa.registry.TokenScope;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class RegistryTokenHandler extends RegistryBase
{
    @Inject
    protected ContainerRepoDb _repoDb;

    public RegistryTokenHandler()
    {

    }

    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        String registryApiToken = requestContext.getRegistryApiToken();
        String ownerDomain = requestContext.getOwnerDomain();
        TokenScope tokenScope = TokenScope.fromString(requestContext.getParameter("scope"));

        if(registryApiToken == null)
        {
            //its an anonymous request
            if(tokenScope.isPullRequest())
                return validatePublicRepo(tokenScope, ownerDomain, requestContext);
            else //its not a pull so return auth error
                RequireAuthError.throwRequireAuth("Invalid username or password",
                                                  requestContext);
        }
        else
        {
            //its an authenticated request. If the token is the PUBLIC
            //TOKEN then the token scope must be for a public repo
            if(tokenScope.isPullRequest())
            {
                //its not the public token so lets just echo it back
                if(!RegistryToken.isPublicToken(registryApiToken))
                    return toJson(RegistryToken.fromString(registryApiToken));
                return validatePublicRepo(tokenScope, ownerDomain, requestContext);
            }
            else //its a login or push
            {
                //If its the public token then reject it
                if(RegistryToken.isPublicToken(registryApiToken))
                    RequireAuthError.throwRequireAuth(
                        "You do not have access to this operation", requestContext);
                //else echo the same token back to the client
                return toJson(RegistryToken.fromString(registryApiToken));
            }
        }
        //Should not have reached here
        return null;
    }

    protected WebResponse validatePublicRepo(TokenScope tokenScope, String ownerDomain, EuropaRequestContext requestContext)
    {
        //validate the the pull is for a public repo
        String repoName = tokenScope.getRepoName();
        //The repo could not be found. So throw an auth error
        if(repoName == null)
            RequireAuthError.throwRequireAuth("Invalid scope", requestContext);

        ContainerRepo repo = getContainerRepo(ownerDomain, repoName);
        //if its a public repo then return the PUBLIC TOKEN
        if(repo != null && repo.isPublicRepo())
            return WebResponse.toJson(RegistryToken.PUBLIC_TOKEN);
        if(log.isDebugEnabled())
            log.debug("Disallowing Repo public Access to repo: "+repoName);
        RequireAuthError.throwRequireAuth("Missing username or password", requestContext);
        //Should not have reached here
        return null;
    }

    protected ContainerRepo getContainerRepo(String ownerDomain, String repoName)
    {
        return _repoDb.getLocalRepo(ownerDomain,
                                    repoName);
    }
}
