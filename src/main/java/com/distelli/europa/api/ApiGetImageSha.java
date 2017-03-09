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
public class ApiGetImageSha extends ApiBase
{
    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    protected ManifestTransformer _manifestTransformer;

    public ApiGetImageSha()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext)
    {
        String repoId = getPathParam("id", requestContext);
        String domain = requestContext.getOwnerDomain();
        String sha = getPathParam("sha", requestContext);

        RegistryManifest manifest = _manifestDb.getManifestByRepoIdManifestId(domain,
                                                                              repoId,
                                                                              sha);
        if(manifest == null)
            throw(new AjaxClientException("The specified sha was not found",
                                          AjaxErrors.Codes.ImageNotFound,
                                          404));
        return toJson(_manifestTransformer.transform(manifest));
    }
}
