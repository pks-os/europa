package com.distelli.europa.handlers;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;

public class RegistryHealthCheck extends RegistryBase {
    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        WebResponse response = new WebResponse(200);
        return response;
    }
}
