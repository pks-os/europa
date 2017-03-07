/*
  $Id: $
  @file ApiBase.java
  @brief Contains the ApiBase.java class

  @author Rahul Singh [rsingh]
  Copyright (c) 2013, Distelli Inc., All Rights Reserved.
*/
package com.distelli.europa.api;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.webserver.AjaxClientException;
import com.distelli.webserver.JsonError;
import com.distelli.webserver.MatchedRoute;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public abstract class ApiBase extends RequestHandler<EuropaRequestContext>
{
    public ApiBase()
    {

    }

    protected String getPathParam(String name, EuropaRequestContext requestContext)
    {
        MatchedRoute route = requestContext.getMatchedRoute();
        return route.getParam(name);
    }

    protected String getParam(String name, EuropaRequestContext requestContext)
    {
        return requestContext.getParameter(name);
    }

    protected int getParamAsInt(String name,
                                int defaultValue,
                                EuropaRequestContext requestContext)
    {
        String value = getParam(name, requestContext);
        if(value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException nfe) {

        }
        return defaultValue;
    }

    public WebResponse handleRequest(EuropaRequestContext requestContext) {
        try {
            return handleApiRequest(requestContext);
        } catch(AjaxClientException ace) {
            JsonError jsonError = ace.getJsonError();
            if(jsonError != null)
                return jsonError(jsonError);
            return jsonError(JsonError.MalformedRequest);
        } catch(Throwable t) {
            log.error(t.getMessage(), t);
            return jsonError(JsonError.InternalServerError);
        }
    }

    public WebResponse handleApiRequest(EuropaRequestContext requestContext) {
        throw(new UnsupportedOperationException());
    }
}
