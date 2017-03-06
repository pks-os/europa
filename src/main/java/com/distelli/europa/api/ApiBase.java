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
        System.out.println("Handling API: "+requestContext);
        return WebResponse.ok("Hello");
    }
}
