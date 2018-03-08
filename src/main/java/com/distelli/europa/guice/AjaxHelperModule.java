/*
  $Id: $
  @file AjaxHelperModule.java
  @brief Contains the AjaxHelperModule.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.guice;

import com.distelli.europa.ajax.AddPipelineComponent;
import com.distelli.europa.ajax.CreateAuthToken;
import com.distelli.europa.ajax.CreateCacheRepo;
import com.distelli.europa.ajax.CreateLocalRepo;
import com.distelli.europa.ajax.DeleteAuthToken;
import com.distelli.europa.ajax.DeleteContainerRepo;
import com.distelli.europa.ajax.DeletePipelineContainerRepoId;
import com.distelli.europa.ajax.DeleteRegistryCreds;
import com.distelli.europa.ajax.DeleteRepoNotification;
import com.distelli.europa.ajax.GetContainerRepo;
import com.distelli.europa.ajax.GetNotificationRecord;
import com.distelli.europa.ajax.GetPipeline;
import com.distelli.europa.ajax.GetRegionsForProvider;
import com.distelli.europa.ajax.GetRepoOverview;
import com.distelli.europa.ajax.GetSslSettings;
import com.distelli.europa.ajax.GetStorageSettings;
import com.distelli.europa.ajax.ListAuthTokens;
import com.distelli.europa.ajax.ListContainerRepos;
import com.distelli.europa.ajax.ListPipelines;
import com.distelli.europa.ajax.ListRegistryCreds;
import com.distelli.europa.ajax.ListRepoEvents;
import com.distelli.europa.ajax.ListRepoManifests;
import com.distelli.europa.ajax.ListRepoNotifications;
import com.distelli.europa.ajax.ListReposInRegistry;
import com.distelli.europa.ajax.MovePipelineComponent;
import com.distelli.europa.ajax.NewPipeline;
import com.distelli.europa.ajax.RedeliverWebhook;
import com.distelli.europa.ajax.RemovePipeline;
import com.distelli.europa.ajax.RemovePipelineComponent;
import com.distelli.europa.ajax.RunPipelineManualPromotion;
import com.distelli.europa.ajax.SaveContainerRepo;
import com.distelli.europa.ajax.SaveGcrServiceAccountCreds;
import com.distelli.europa.ajax.SaveRegistryCreds;
import com.distelli.europa.ajax.SaveRepoNotification;
import com.distelli.europa.ajax.SaveRepoOverview;
import com.distelli.europa.ajax.SaveSslSettings;
import com.distelli.europa.ajax.SaveStorageSettings;
import com.distelli.europa.ajax.SetAuthTokenStatus;
import com.distelli.europa.ajax.SetPipelineContainerRepoId;
import com.distelli.europa.ajax.SetRepoPublic;
import com.distelli.europa.ajax.TestWebhookDelivery;
import com.distelli.europa.ajax.UpdateStorageCreds;
import com.distelli.webserver.AjaxHelper;
import com.distelli.webserver.AjaxHelperMap;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import lombok.extern.log4j.Log4j;

import java.util.HashSet;
import java.util.Set;

@Log4j
public class AjaxHelperModule extends AbstractModule
{
    public AjaxHelperModule()
    {

    }

    protected void configure()
    {
        // Add ajax bindings here
        addBinding(ListPipelines.class);
        addBinding(GetPipeline.class);
        addBinding(SetPipelineContainerRepoId.class);
        addBinding(DeletePipelineContainerRepoId.class);
        addBinding(NewPipeline.class);
        addBinding(RemovePipeline.class);
        addBinding(AddPipelineComponent.class);
        addBinding(MovePipelineComponent.class);
        addBinding(RemovePipelineComponent.class);
        addBinding(RunPipelineManualPromotion.class);

        addBinding(GetRegionsForProvider.class);
        addBinding(ListReposInRegistry.class);

        //Cred CRUD helpers
        addBinding(SaveRegistryCreds.class);
        addBinding(ListRegistryCreds.class);
        addBinding(DeleteRegistryCreds.class);
        addBinding(SaveGcrServiceAccountCreds.class);

        //Container CRUD helpers
        addBinding(SaveContainerRepo.class);
        addBinding(CreateLocalRepo.class);
        addBinding(CreateCacheRepo.class);
        addBinding(GetContainerRepo.class);
        addBinding(ListContainerRepos.class);
        addBinding(DeleteContainerRepo.class);
        addBinding(TestWebhookDelivery.class);
        addBinding(ListRepoEvents.class);
        addBinding(ListRepoManifests.class);
        addBinding(SaveRepoNotification.class);
        addBinding(DeleteRepoNotification.class);
        addBinding(ListRepoNotifications.class);
        addBinding(GetNotificationRecord.class);
        addBinding(RedeliverWebhook.class);

        //Token CRUD helpers
        addBinding(ListAuthTokens.class);
        addBinding(CreateAuthToken.class);
        addBinding(SetAuthTokenStatus.class);
        addBinding(DeleteAuthToken.class);

        addBinding(SaveStorageSettings.class);
        addBinding(GetStorageSettings.class);
        addBinding(UpdateStorageCreds.class);
        addBinding(GetRepoOverview.class);
        addBinding(SaveRepoOverview.class);
        addBinding(SetRepoPublic.class);

        addBinding(SaveSslSettings.class);
        addBinding(GetSslSettings.class);

        bind(AjaxHelperMap.class).to(AjaxHelperMapImpl.class);
    }

    protected void addBinding(Class<? extends AjaxHelper> clazz)
    {
        addBinding(clazz, null);
    }

    protected void addBinding(Class<? extends AjaxHelper> clazz, String... paths) {
        addBinding(clazz.getSimpleName(), clazz, paths);
    }

    protected void addBinding(String operationName, Class<? extends AjaxHelper> clazz, String... paths)
    {
        MapBinder<String, AjaxHelper> mapbinder = MapBinder.newMapBinder(binder(), String.class, AjaxHelper.class);
        mapbinder.addBinding(operationName).to(clazz);

        MapBinder<String, Set<String>> pathRestrictionBinder = MapBinder.newMapBinder(binder(),
                                                                                      new TypeLiteral<String>(){},
                                                                                      new TypeLiteral<Set<String>>(){});
        if(paths != null)
        {
            Set<String> pathSet = new HashSet<String>();
            for(String path : paths)
                pathSet.add(path);
            pathRestrictionBinder.addBinding(operationName).toInstance(pathSet);
        }
    }
}
