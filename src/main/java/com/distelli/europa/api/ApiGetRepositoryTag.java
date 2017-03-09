/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import javax.inject.Singleton;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.ajax.AjaxErrors;
import com.distelli.europa.api.models.*;
import com.distelli.europa.api.transformers.*;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.WebResponse;

@Log4j
@Singleton
public class ApiGetRepositoryTag extends ApiBase
{
    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    protected ManifestTransformer _manifestTransformer;

    public ApiGetRepositoryTag()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext)
    {
        String repoId = getPathParam("id", requestContext);
        String domain = requestContext.getOwnerDomain();
        String tag = getPathParam("tag", requestContext);

        RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(domain,
                                                                       repoId,
                                                                       tag);
        if(manifest == null)
            throw(new AjaxClientException("The specified tag was not found",
                                          AjaxErrors.Codes.TagNotFound,
                                          404));
        return toJson(_manifestTransformer.transform(manifest));
    }
}
