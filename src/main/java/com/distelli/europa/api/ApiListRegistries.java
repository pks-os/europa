/*
  $Id: $
  @author Rahul Singh [rsingh]
  Copyright (c) 2017, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.Constants;
import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.ajax.AjaxErrors;
import com.distelli.europa.api.models.*;
import com.distelli.europa.api.transformers.*;
import com.distelli.europa.db.RegistryCredsDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.db.RepoEventsDb;
import com.distelli.europa.models.RegistryCred;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class ApiListRegistries extends ApiBase
{
    @Inject
    private RegistryCredsDb _db;
    @Inject
    private RegistryTransformer _registryTransformer;

    public ApiListRegistries()
    {

    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext)
    {
        int pageSize = getParamAsInt("pageSize", 100, requestContext);
        String marker = getParam("marker", requestContext);
        String order = getParam("order", requestContext);
        String domain = requestContext.getOwnerDomain();
        boolean dbAscending = false;
        if(order == null)
            order = Constants.API_ORDER_ASCENDING;

        if(order.equals(Constants.API_ORDER_ASCENDING))
            dbAscending = false;
        else if(order.equals(Constants.API_ORDER_DESCENDING))
            dbAscending = true;
        else
            throw(new AjaxClientException("Invalid value for parameter: order",
                                          JsonError.Codes.BadParam,
                                          400));

        PageIterator iter = new PageIterator()
        .pageSize(pageSize)
        .marker(marker);
        if(!dbAscending)
            iter.backward();

        List<RegistryCred> creds = _db.listAllCreds(domain, iter);
        List<ApiRegistry> registries = _registryTransformer.transform(creds);
        if(dbAscending)
            Collections.reverse(registries);

        ApiRegistryList registryList = ApiRegistryList
        .builder()
        .registries(registries)
        .prevMarker(dbAscending ? iter.getMarker() : iter.getPrevMarker())
        .nextMarker(dbAscending ? iter.getPrevMarker() : iter.getMarker())
        .build();
        return toJson(registryList);
    }
}
