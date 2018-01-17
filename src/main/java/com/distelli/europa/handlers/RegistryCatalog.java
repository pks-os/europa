package com.distelli.europa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.WebResponse;
import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class RegistryCatalog extends RegistryBase {
    private static int DEFAULT_PAGE_SIZE = 100;
    @Inject
    private ContainerRepoDb _reposDb;

    private static class Response {
        public List<String> repositories = new ArrayList<String>();
    }

    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        String ownerUsername = requestContext.getOwnerUsername();
        String ownerDomain = requestContext.getOwnerDomain();
        PageIterator pageIterator = new PageIterator()
            .pageSize(getPageSize(requestContext))
            .marker(requestContext.getParameter("last"));

        List<ContainerRepo> repoList = listRepositories(ownerUsername, ownerDomain, pageIterator);

        Map<ContainerRepo, Boolean> permissionResult = _permissionCheck.checkBatch(this.getClass().getSimpleName(),
                                                                                   requestContext,
                                                                                   repoList);
        Response response = new Response();
        Map<String, String> usernameCache = new HashMap<>();
        usernameCache.put(ownerDomain, ownerUsername);
        for(ContainerRepo repo : repoList)
        {
            boolean allow = repo.isPublicRepo();
            if(!allow)
                allow = permissionResult.get(repo);
            if(allow)
            {
                String repoName = joinWithSlash(getUsername(usernameCache, repo.getDomain()), repo.getName());
                response.repositories.add(repoName);
            }
        }

        String location = null;
        if ( null != pageIterator.getMarker() ) {
            location = joinWithSlash("/v2", ownerUsername, "_catalog") + "?last=" + pageIterator.getMarker();
            if ( DEFAULT_PAGE_SIZE != pageIterator.getPageSize() ) {
                location = location + "&n="+pageIterator.getPageSize();
            }
        }

        WebResponse webResponse = toJson(response);
        if ( null != location ) {
            webResponse.setResponseHeader("Link", location + "; rel=\"next\"");
        }
        return webResponse;
    }

    protected List<ContainerRepo> listRepositories(String ownerUsername, String ownerDomain, PageIterator pageIterator) {
        return _reposDb.listEuropaRepos(ownerDomain, pageIterator);
    }

    protected String getUsername(Map<String, String> usernameCache, String domain) {
        return usernameCache.get(domain);
    }
}
