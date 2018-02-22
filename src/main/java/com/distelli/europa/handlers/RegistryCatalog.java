package com.distelli.europa.handlers;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.WebResponse;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        Response response = new Response();

        Map<String, String> usernameCache = new HashMap<>();
        usernameCache.put(ownerDomain, ownerUsername);

        // In order for pagination to work properly with access control, we
        // handle it manually, since the DB doesn't know anything about it.
        for (PageIterator iter : pageIterator) {
            List<ContainerRepo> repos = listRepositories(ownerUsername, ownerDomain, iter);
            Map<ContainerRepo, Boolean> permissionResult = _permissionCheck.checkBatch(this.getClass().getSimpleName(),
                                                                                       requestContext,
                                                                                       repos);
            List<ContainerRepo> filteredRepos = repos.stream()
                .filter(repo -> (repo.isPublicRepo() || permissionResult.get(repo).equals(Boolean.TRUE)))
                .collect(Collectors.toList());
            List<String> filteredRepoNames = filteredRepos.stream()
                .map(repo -> joinWithSlash(getUsername(usernameCache, repo.getDomain()),
                                           repo.getName()))
                .collect(Collectors.toList());
            if (response.repositories.size() + filteredRepos.size() >= pageIterator.getPageSize()) {
                int nextIndex = pageIterator.getPageSize() - response.repositories.size();
                response.repositories.addAll(filteredRepoNames.subList(0, nextIndex));
                if (iter.getMarker() != null) {
                    pageIterator.setMarker(getMarker(filteredRepos.get(nextIndex - 1), ownerUsername));
                }
                break;
            } else {
                response.repositories.addAll(filteredRepoNames);
            }
        }

        WebResponse webResponse = toJson(response);

        if (ownerUsername != null) {
            addPaginationLinkHeader(webResponse, pageIterator, "v2", ownerUsername, "_catalog");
        } else {
            addPaginationLinkHeader(webResponse, pageIterator, "v2", "_catalog");
        }

        return webResponse;
    }

    protected List<ContainerRepo> listRepositories(String ownerUsername, String ownerDomain, PageIterator pageIterator) {
        return _reposDb.listEuropaRepos(ownerDomain, pageIterator);
    }

    protected String getUsername(Map<String, String> usernameCache, String domain) {
        return usernameCache.get(domain);
    }

    protected String getMarker(ContainerRepo repo, String ownerUsername) {
        return _reposDb.getSecondaryIndexMarker(repo);
    }
}
