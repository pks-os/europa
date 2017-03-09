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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public abstract class ApiBase extends RequestHandler<EuropaRequestContext>
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

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

    protected <T> T getContent(Class<T> clazz, EuropaRequestContext requestContext, boolean throwIfNull)
    {
        JsonNode jsonContent = requestContext.getJsonContent();
        if(jsonContent != null)
        {
            T contentObj = OBJECT_MAPPER.convertValue(jsonContent, clazz);
            if(contentObj != null)
                return contentObj;
        }
        if(throwIfNull)
            throw(new AjaxClientException(JsonError.BadContent));
        return null;
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
