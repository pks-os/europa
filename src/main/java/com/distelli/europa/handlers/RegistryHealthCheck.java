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


public class RegistryHealthCheck extends RequestHandler<EuropaRequestContext> {
    @Inject
    private RegistryBlobDb _blobDb;
    @Inject
    private Provider<ObjectStore> _objectStoreProvider;
    @Inject
    private Provider<ObjectKeyFactory> _objectKeyFactoryProvider;
    @Inject
    private ExecutorService _executorService = Executors.newSingleThreadExecutor();

    private  class checkHealth implements Callable{

        @Override
        public WebResponse call() throws Exception {

            // Check DB connection
            RegistryBlob blob = null;
            try {
                blob = _blobDb.getRegistryBlobById("DNE");
            } catch (Exception e) {
                System.out.println(e);
                WebResponse response = new WebResponse(503, "Database connection failed");
                response.setContentType("application/json");
                return response;
            }

            //Check Object Store connection
            ObjectKeyFactory objectKeyFactory = _objectKeyFactoryProvider.get();
            ObjectKey objKey = objectKeyFactory.forRegistryBlobId(blob.getBlobId());
            ObjectStore objectStore = _objectStoreProvider.get();
            // Check that object store is consistent with DB:
            ObjectMetadata meta = objectStore.head(objKey);
            if ( null == meta ) {
                WebResponse response = new WebResponse(503, "Object store connection failed");
                response.setContentType("application/json");
                return response;
            }

            WebResponse response = new WebResponse(200, "ok");
            response.setContentType("application/json");
            return response;

        }
    }

    public WebResponse handleRequest(EuropaRequestContext requestContext) {

        /*if (null == blob) {
            WebResponse response = new WebResponse(503, "Database connection failed");
            response.setContentType("application/json");
            return response;
        }
        //Check Object Store connection
        ObjectKeyFactory objectKeyFactory = _objectKeyFactoryProvider.get();
        ObjectKey objKey = objectKeyFactory.forRegistryBlobId(blob.getBlobId());
        ObjectStore objectStore = _objectStoreProvider.get();
        // Check that object store is consistent with DB:
        ObjectMetadata meta = objectStore.head(objKey);
        if ( null == meta ) {
            WebResponse response = new WebResponse(503, "Object store connection failed");
            response.setContentType("application/json");
            return response;
        }
        WebResponse response = new WebResponse(200, "ok");
        response.setContentType("application/json");
        return response;*/
        Future<WebResponse> healthFuture = _executorService.submit(new checkHealth());
        try {
            healthFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        return null;
    }
}
