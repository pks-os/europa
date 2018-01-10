package com.distelli.europa.filters;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.models.SslSettings;
import com.distelli.webserver.RequestFilter;
import com.distelli.webserver.RequestFilterChain;
import com.distelli.webserver.WebConstants;
import com.distelli.webserver.WebResponse;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

@Log4j
public class ForceHttpsFilter implements RequestFilter<EuropaRequestContext>
{
    @Inject
    protected Provider<SslSettings> _sslSettingsProvider;

    public ForceHttpsFilter()
    {

    }

    @Override
    public WebResponse filter(EuropaRequestContext requestContext, RequestFilterChain next)
    {
        String protocol = requestContext.getProto();
        String path = requestContext.getPath();
        // The health check endpoint needs to work over HTTP
        if(protocol.equalsIgnoreCase("http") && !path.equalsIgnoreCase("/healthz")) {
            SslSettings sslSettings = _sslSettingsProvider.get();
            if (sslSettings.getForceHttps()) {
                List<String> newLocationParts = new ArrayList<>();
                newLocationParts.add("https://");
                String hostName = sslSettings.getDnsName();
                if (null == hostName) {
                    hostName = requestContext.getHost("");
                }
                newLocationParts.add(hostName);
                newLocationParts.add(requestContext.getOriginalPath());
                if (null != requestContext.getQueryString()) {
                    newLocationParts.add("?");
                    newLocationParts.add(requestContext.getQueryString());
                }
                String newLocation = String.join("", newLocationParts);
                WebResponse redirectResponse = new WebResponse(307);
                redirectResponse.setResponseHeader(WebConstants.LOCATION_HEADER, newLocation);
                return redirectResponse;
            }
        }
        return next.filter(requestContext);
    }
}
