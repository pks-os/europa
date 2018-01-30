package com.distelli.europa.filters;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;
import com.distelli.europa.security.HandlerAccessAuthorization;
import com.distelli.webserver.RequestFilter;
import com.distelli.webserver.RequestFilterChain;
import com.distelli.webserver.WebResponse;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;

@Log4j
public class RegistryApiAuthorizationFilter implements RequestFilter<EuropaRequestContext> {
    @Inject
    protected HandlerAccessAuthorization _handlerAccessAuthorization;
    @Inject
    protected ContainerRepoDb _repoDb;

    @Override
    public WebResponse filter(EuropaRequestContext requestContext, RequestFilterChain<EuropaRequestContext> next) {
        if (requestContext.isRegistryApiRequest()) {
            String requesterDomain = requestContext.getRequesterDomain();
            String ownerDomain = requestContext.getOwnerDomain();
            String repoName = requestContext.getMatchedRoute().getParam("name");
            boolean authorized;
            if (requesterDomain == null) {
                throw (new RegistryError("Missing Authorization header",
                                         RegistryErrorCode.UNAUTHORIZED));
            }
            if (repoName == null || ownerDomain == null) {
                authorized = _handlerAccessAuthorization.checkAuthorization(requesterDomain,
                                                                            requestContext.getMatchedRoute().getRequestHandler());
            } else {
                ContainerRepo repo = _repoDb.getLocalRepo(ownerDomain,
                                                          repoName);
                authorized = _handlerAccessAuthorization.checkAuthorization(requesterDomain,
                                                                            requestContext.getMatchedRoute().getRequestHandler(),
                                                                            repo);
            }

            if (!authorized) {
                throw (new RegistryError("You do not have access to this operation",
                                         RegistryErrorCode.DENIED));
            }
        }
        return next.filter(requestContext);
    }
}
