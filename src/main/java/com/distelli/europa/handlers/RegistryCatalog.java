package com.distelli.europa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.distelli.europa.EuropaRequestContext;
import lombok.Getter;
import org.eclipse.jetty.http.HttpMethod;
import com.distelli.europa.EuropaConfiguration;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.RequestContext;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j;

@Log4j
@Singleton
public class RegistryCatalog extends RegistryBase {
    private static int DEFAULT_PAGE_SIZE = 100;
    @Inject
    protected ContainerRepoDb _reposDb;

    private static class Response {
        public List<String> repositories = new ArrayList<String>();
    }

    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        String ownerUsername = requestContext.getOwnerUsername();
        String ownerDomain = requestContext.getOwnerDomain();
        PageIterator pageIterator = new PageIterator()
            .pageSize(getPageSize(requestContext))
            .marker(requestContext.getParameter("last"));

        List<RepoOwnerUsernameMap> repoList = listRepositories(ownerUsername, ownerDomain, pageIterator);

        Map<ContainerRepo, Boolean> permissionResult = _permissionCheck.checkBatch(this.getClass().getSimpleName(),
                                                                                   requestContext,
                                                                                   repoList
                                                                                       .stream()
                                                                                       .map((x) -> x.getRepo())
                                                                                       .collect(Collectors.toList()));
        Response response = new Response();
        for(RepoOwnerUsernameMap repoData : repoList)
        {
            String repoOwnerUsername = repoData.getOwner();
            ContainerRepo repo = repoData.getRepo();
            boolean allow = repo.isPublicRepo();
            if(!allow)
                allow = permissionResult.get(repo);
            if(allow)
            {
                String repoName = joinWithSlash(repoOwnerUsername, repo.getName());
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

    protected List<RepoOwnerUsernameMap> listRepositories(String ownerUsername, String ownerDomain, PageIterator pageIterator) {
        return _reposDb.listEuropaRepos(ownerDomain, pageIterator)
            .stream()
            .map((repo) -> new RepoOwnerUsernameMap(ownerUsername, repo))
            .collect(Collectors.toList());
    }

    protected class RepoOwnerUsernameMap {
        @Getter
        private final String owner;
        @Getter
        private final ContainerRepo repo;

        public RepoOwnerUsernameMap(String ownerUsername, ContainerRepo containerRepo)
        {
            owner = ownerUsername;
            repo = containerRepo;
        }
    }
}
