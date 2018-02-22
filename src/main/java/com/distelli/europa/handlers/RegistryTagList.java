package com.distelli.europa.handlers;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.persistence.PageIterator;
import com.distelli.webserver.WebResponse;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j
@Singleton
public class RegistryTagList extends RegistryBase {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^sha256:[0-9a-fA-F]{64}$");
    @Inject
    private RegistryManifestDb _manifestDb;
    private static class Response {
        public String name;
        public List<String> tags;
    }
    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        String ownerUsername = requestContext.getOwnerUsername();
        String ownerDomain = requestContext.getOwnerDomain();
        String name = requestContext.getMatchedRoute().getParam("name");

        ContainerRepo repo = getContainerRepo(ownerDomain, name);
        if ( null == repo ) {
            Response response = new Response();
            response.name = joinWithSlash(ownerUsername, name);
            response.tags = Collections.emptyList();
            return toJson(response);
        }

        PageIterator it = new PageIterator()
            .pageSize(getPageSize(requestContext))
            .marker(requestContext.getParameter("last"));
        List<RegistryManifest> manifests = new ArrayList<>();
        while ( it.hasNext() && manifests.size() < it.getPageSize() ) {
            _manifestDb.listManifestsByRepoId(ownerDomain, repo.getId(), it).stream()
                .filter((manifest) -> !SHA256_PATTERN.matcher(manifest.getTag()).matches())
                .forEachOrdered(manifests::add);
        }

        Response response = new Response();
        response.name = joinWithSlash(ownerUsername, name);
        response.tags = manifests.stream()
            .map((manifest) -> manifest.getTag())
            .collect(Collectors.toList());

        WebResponse webResponse = toJson(response);
        addPaginationLinkHeader(webResponse, it, "v2", ownerUsername, name, "tags", "list");
        return webResponse;
    }
}
