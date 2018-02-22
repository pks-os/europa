package com.distelli.europa.handlers;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.guice.ObjectStoreNotInitialized;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;
import com.distelli.europa.util.PermissionCheck;
import com.distelli.persistence.PageIterator;
import com.distelli.utils.CompactUUID;
import com.distelli.webserver.RequestContext;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.StringJoiner;

@Log4j
@Singleton
public abstract class RegistryBase extends RequestHandler<EuropaRequestContext>
{
    protected static int DEFAULT_PAGE_SIZE = 100;

    abstract public WebResponse handleRegistryRequest(EuropaRequestContext requestContext);

    private static final ObjectMapper OM = new ObjectMapper();

    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private ContainerRepo _repo;
    @Inject
    protected PermissionCheck _permissionCheck;

    public WebResponse handleRequest(EuropaRequestContext requestContext) {
        try {
            String operationName = this.getClass().getSimpleName();
            if(_permissionCheck != null)
                _permissionCheck.checkRegistryAccess(operationName, requestContext);
            return handleRegistryRequest(requestContext);
        } catch ( RegistryError ex ) {
            return handleError(ex);
        } catch ( ObjectStoreNotInitialized osni ) {
            return handleError(new RegistryError("Europa Storage not initialized. Please contact your administrator",
                                                 RegistryErrorCode.UNSUPPORTED));
        } catch ( Throwable ex ) {
            log.error(ex.getMessage(), ex);
            return handleError(new RegistryError(ex));
        }
    }

    private WebResponse handleError(RegistryError error) {
        if ( log.isInfoEnabled() ) {
            try {
                log.info("RegistryError: "+error.getErrorCode()+" "+OM.writeValueAsString(error.getResponseBody()));
            } catch ( Exception ex ){
                log.error(ex.getMessage(), ex);
            }
        }
        WebResponse response = toJson(error.getResponseBody(), error.getStatusCode());
        for ( Map.Entry<String, String> entry : error.getResponseHeaders().entrySet() ) {
            response.setResponseHeader(entry.getKey(), entry.getValue());
        }
        return response;
    }

    protected static void pump(InputStream in, OutputStream out) throws IOException {
        byte[] buff = new byte[1024*1024];
        while ( true ) {
            int len=in.read(buff);
            if ( len <= 0 ) break;
            out.write(buff, 0, len);
        }
    }

    protected String joinWithSlash(String... parts) {
        if ( null == parts || parts.length <= 0 ) return "";
        StringJoiner joiner = new StringJoiner("/");
        for ( String part : parts ) {
            if ( null == part ) continue;
            joiner.add(part);
        }
        return joiner.toString();
    }

    /**
     * Sets the Link HTTP header for pagination, if applicable
     *
     * If {@code pageIterator.getMarker()} returns {@code null}, this does
     * nothing. Otherwise, it adds a {@code Link} header to the provided
     * WebResponse object and returns the object again. Note that you need to
     * specify all the components of the path, e.g.,
     * <p>
     * {@code addPaginationLinkHeader(webResponse, pageIterator, "v2", ownerUsername, "_catalog")}
     *
     * @param webResponse the WebResponse to add the header to
     * @param pageIterator the PageIterator used for pagination
     * @param pathComponents the components of the path to use, including v2
     * @return the WebResponse
     */
    protected WebResponse addPaginationLinkHeader(WebResponse webResponse,
                                                  PageIterator pageIterator,
                                                  String... pathComponents) {
        // The format for a Link header, at least how we need to use it, is:
        //     Link: </uri/path/to/resource?foo=bar;baz=1>; rel=next
        // Note that the URI is surrounded by angle brackets, per RFC 5988.
        if (pageIterator.getMarker() != null) {
            StringBuilder linkUriBuilder = new StringBuilder("/");
            linkUriBuilder.append(String.join("/", pathComponents));
            linkUriBuilder.append("?last=");
            linkUriBuilder.append(pageIterator.getMarker());
            if (DEFAULT_PAGE_SIZE != pageIterator.getPageSize()) {
                linkUriBuilder.append("&n=");
                linkUriBuilder.append(pageIterator.getPageSize());
            }
            linkUriBuilder.insert(0, "<");
            linkUriBuilder.append(">; rel=\"next\"");
            webResponse.setResponseHeader("Link", linkUriBuilder.toString());
        }
        return webResponse;
    }

    protected int getPageSize(RequestContext requestContext) {
        try {
            return Integer.parseInt(requestContext.getParameter("n"));
        } catch ( NumberFormatException ex ) {}
        return DEFAULT_PAGE_SIZE;
    }

    protected ContainerRepo getContainerRepo(String domain, String repoName) {
        return _repoDb.getLocalRepo(domain, repoName);
    }

    protected ContainerRepo getOrCreateContainerRepo(String domain, String repoName) {
        ContainerRepo repo = getContainerRepo(domain, repoName);
        if ( null != repo ) return repo;
        repo = ContainerRepo.builder()
            .domain(domain)
            .name(repoName)
            .region("")
            .provider(RegistryProvider.EUROPA)
            .local(true)
            .publicRepo(false)
            .build();

        repo.setOverviewId(CompactUUID.randomUUID().toString());
        repo.setId(CompactUUID.randomUUID().toString());
        _repoDb.save(repo);
        // Re-fetch to avoid race condition:
        return getOrCreateContainerRepo(domain, repoName);
    }
}
