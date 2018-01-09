package com.distelli.europa.handlers;

import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.models.RegistryBlob;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;
import com.distelli.europa.util.ObjectKeyFactory;
import com.distelli.objectStore.ObjectKey;
import com.distelli.objectStore.ObjectMetadata;
import com.distelli.objectStore.ObjectStore;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;
import com.distelli.europa.db.RegistryBlobDb;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RegistryHealthCheck extends RequestHandler<EuropaRequestContext> {
    @Inject
    private RegistryBlobDb _blobDb;
    @Inject
    private Provider<ObjectStore> _objectStoreProvider;
    @Inject
    private Provider<ObjectKeyFactory> _objectKeyFactoryProvider;
    @Inject
    private ExecutorService _executorService;

    public WebResponse handleRequest(EuropaRequestContext requestContext) {
        Future<?> healthFuture = _executorService.submit(
            () -> {
                // Try to round-trip the DB:
                _blobDb.getRegistryBlobById("DNE");

                // Try to round-trip the object store:
                ObjectKeyFactory objectKeyFactory = _objectKeyFactoryProvider.get();
                ObjectKey objKey = objectKeyFactory.forRegistryBlobId("DNE");

                // Check that object store is consistent with DB:
                _objectStoreProvider.get().head(objKey);
            });
        try {
            healthFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            WebResponse response = new WebResponse(503);
            response.setContentType("text/plain");
            response.setResponseContent(ex.getMessage().getBytes(UTF_8));
            return response;
        }

        WebResponse response = new WebResponse(200, "ok\n");
        response.setContentType("text/plain");
        return response;
    }
}
