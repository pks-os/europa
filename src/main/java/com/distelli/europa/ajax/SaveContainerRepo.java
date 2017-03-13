/*
  $Id: $
  @file SaveContainerRepo.java
  @brief Contains the SaveContainerRepo.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.ajax;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Provider;

import java.net.MalformedURLException;
import java.net.URL;

import com.distelli.utils.CompactUUID;
import com.distelli.europa.clients.*;
import com.distelli.europa.db.*;
import com.distelli.europa.models.*;
import com.distelli.europa.util.*;
import com.distelli.webserver.*;
import com.distelli.gcr.*;
import com.distelli.gcr.auth.*;
import com.distelli.gcr.models.*;
import com.distelli.persistence.PageIterator;
import com.google.inject.Singleton;
import org.eclipse.jetty.http.HttpMethod;
import lombok.extern.log4j.Log4j;
import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.helpers.RepositoryCreator;

@Log4j
@Singleton
public class SaveContainerRepo extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    private ContainerRepoDb _reposDb;
    @Inject
    private NotificationsDb _notificationDb;
    @Inject
    protected PermissionCheck _permissionCheck;
    @Inject
    protected RepositoryCreator _repoCreator;

    public SaveContainerRepo()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        _permissionCheck.check(ajaxRequest.getOperation(), requestContext);
        ContainerRepo repo = ajaxRequest.convertContent("/repo", ContainerRepo.class,
                                                       true); //throw if null
        //Validate that the fields we want are non-null
        FieldValidator.validateNonNull(repo, "credId", "name");
        String repoDomain = requestContext.getOwnerDomain();
        repo = _repoCreator.createRemoteRepo(repoDomain, repo.getName(), repo.getCredId());

        Notification notification = ajaxRequest.convertContent("/notification", Notification.class, false);
        if(notification != null) {
            FieldValidator.validateNonNull(notification, "type", "target");

            try {
                URL url = new URL(notification.getTarget());
            } catch(MalformedURLException mue) {
                throw(new AjaxClientException("Invalid Target URL on Webhook Notification: "+notification.getTarget(),
                                              JsonError.Codes.BadContent, 400));
            }

            notification.setRepoId(repo.getId());
            notification.setDomain(repoDomain);
            notification.setRepoProvider(repo.getProvider());
            notification.setRegion(repo.getRegion());
            notification.setRepoName(repo.getName());
        }
        //save the repo in the db
        _reposDb.save(repo);

        if(notification != null)
            _notificationDb.save(notification);

        return repo;
    }
}
