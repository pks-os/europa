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

@Log4j
public class ForceHttpsFilter implements RequestFilter<EuropaRequestContext>
{
    @Inject
    protected Provider<SslSettings> _sslSettingsProvider;
    @Inject
    private HttpAlwaysAllowedPaths _httpAlwaysAllowedPaths;

    public ForceHttpsFilter()
    {

    }

    @Override
    public WebResponse filter(EuropaRequestContext requestContext, RequestFilterChain next)
    {
        String protocol = requestContext.getProto();
        String path = requestContext.getPath();
        // The health check endpoint needs to work over HTTP
        if(protocol.equalsIgnoreCase("http") && !_httpAlwaysAllowedPaths.isHttpAlwaysAllowedPath(path)) {
            SslSettings sslSettings = _sslSettingsProvider.get();
            if (sslSettings != null && sslSettings.getForceHttps()) {
                StringBuilder newLocation = new StringBuilder();
                newLocation.append("https://");
                String hostName = sslSettings.getDnsName();
                if (null == hostName) {
                    hostName = requestContext.getHost("");
                }
                newLocation.append(hostName);
                newLocation.append(requestContext.getOriginalPath());
                if (null != requestContext.getQueryString()) {
                    newLocation.append("?");
                    newLocation.append(requestContext.getQueryString());
                }
                WebResponse redirectResponse = new WebResponse(307);
                redirectResponse.setResponseHeader(WebConstants.LOCATION_HEADER, newLocation.toString());
                return redirectResponse;
            }
        }
        return next.filter(requestContext);
    }
}
