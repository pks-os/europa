package com.distelli.europa.handlers;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.eclipse.jetty.http.HttpMethod;
import com.distelli.europa.EuropaRequestContext;
import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.PipelineDb;
import com.distelli.europa.db.RegistryBlobDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.db.RepoEventsDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.DockerImage;
import com.distelli.europa.models.Pipeline;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.models.RegistryProvider;
import com.distelli.europa.models.RepoEvent;
import com.distelli.europa.models.RepoEventType;
import com.distelli.europa.models.UnknownDigests;
import com.distelli.europa.pipeline.RunPipeline;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;
import com.distelli.europa.util.ObjectKeyFactory;
import com.distelli.objectStore.ObjectKey;
import com.distelli.objectStore.ObjectStore;
import com.distelli.persistence.PageIterator;
import com.distelli.utils.CompactUUID;
import com.distelli.utils.CountingInputStream;
import com.distelli.utils.ResettableInputStream;
import com.distelli.utils.ResettableInputStream;
import com.distelli.webserver.RequestContext;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.mrbean.MrBeanModule;
import com.google.inject.Injector;

import lombok.extern.log4j.Log4j;
import static javax.xml.bind.DatatypeConverter.printHexBinary;

@Log4j
@Singleton
public class RegistryManifestPush extends RegistryBase {
    private static ObjectMapper OM = new ObjectMapper();

    static {
        // Support deserializing interfaces:
        OM.registerModule(new MrBeanModule());
        OM.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OM.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        OM.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    }

    @Inject
    private Provider<ObjectKeyFactory> _objectKeyFactoryProvider;
    @Inject
    private Provider<ObjectStore> _objectStoreProvider;
    @Inject
    private RegistryManifestDb _manifestDb;
    @Inject
    private RegistryBlobDb _blobDb;
    @Inject
    private RepoEventsDb _eventDb;
    @Inject
    private ContainerRepoDb _repoDb;
    @Inject
    private PipelineDb _pipelineDb;
    @Inject
    private RunPipeline _runPipeline;

    public WebResponse handleRegistryRequest(EuropaRequestContext requestContext) {
        try {
            return handleRegistryRequestThrows(requestContext);
        } catch ( RuntimeException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }
    public WebResponse handleRegistryRequestThrows(EuropaRequestContext requestContext) throws Exception {
        String ownerDomain = requestContext.getOwnerDomain();
        String name = requestContext.getMatchedRoute().getParam("name");
        String reference = requestContext.getMatchedRoute().getParam("reference");
        // TODO: Validate name and reference.

        ContainerRepo repo = getOrCreateContainerRepo(ownerDomain, name);

        InputStream is = new ResettableInputStream(requestContext.getRequestStream());
        CountingInputStream counter = new CountingInputStream(is);

        // Find the stream size and checksum, validate the layers exist:
        MessageDigest digestCalc = MessageDigest.getInstance("SHA-256");
        DigestInputStream digestStream = new DigestInputStream(counter, digestCalc);

        JsonNode manifestJson = OM.readTree(digestStream);
        Set<String> digests = getDigests(manifestJson);

        long contentLength = counter.getCount();
        is.reset();
        String finalDigest = "sha256:" + printHexBinary(digestCalc.digest()).toLowerCase();

        ObjectKeyFactory objectKeyFactory = _objectKeyFactoryProvider.get();
        ObjectKey objKey = objectKeyFactory.forRegistryManifest(finalDigest);
        ObjectStore objectStore = _objectStoreProvider.get();
        if ( null == objectStore.head(objKey) ) {
            objectStore.put(objKey, contentLength, is);
        } else {
            objKey = null;
        }

        boolean success = false;
        RegistryManifest oldManifest = null;
        long pushTime = System.currentTimeMillis();
        RegistryManifest manifest = RegistryManifest.builder()
            .uploadedBy(requestContext.getRequesterDomain())
            .contentType(getContentType(manifestJson, requestContext.getContentType()))
            .manifestId(finalDigest)
            .domain(repo.getDomain())
            .containerRepoId(repo.getId())
            .tag(reference)
            .digests(digests)
            .pushTime(pushTime)
            .build();

        try {
            // Always write a reference to support pulling via @sha256:...
            _manifestDb.put(manifest.toBuilder()
                            .tag(finalDigest)
                            .build());
            oldManifest = _manifestDb.put(manifest);
            success = true;
        } catch ( UnknownDigests ex ) {
            // TODO: make this be a list of digests...
            throw new RegistryError("Invalid digest(s), are unknown"+ex.getDigests(),
                                    RegistryErrorCode.BLOB_UNKNOWN,
                                    400);
        } finally {
            if ( ! success ) {
                if ( null != objKey ) objectStore.delete(objKey);
            }
        }

        WebResponse response = new WebResponse();
        response.setHttpStatusCode(201);
        response.setResponseHeader("Location", joinWithSlash("/v2", name, "manifests", finalDigest));
        response.setResponseHeader("Docker-Content-Digest", finalDigest);
        return response;
    }

    private String getImageId(JsonNode manifest) {
        // schemaVersion=2
        JsonNode digest = manifest.at("/config/digest");
        if ( digest.isTextual() ) return digest.asText();
        return null;
    }

    private String getContentType(JsonNode manifest, String contentType) {
        if ( null != contentType ) return contentType;
        JsonNode node = manifest.at("/mediaType");
        if ( node.isTextual() ) return node.asText();
        return "application/vnd.docker.distribution.manifest.v1+json";
    }

    private Set<String> getDigests(JsonNode manifest) {
        Set<String> digests = new TreeSet<>();
        if ( manifest.at("/layers").isArray() ) {
            JsonNode node = manifest.at("/config/digest");
            if ( node.isTextual() ) {
                digests.add(node.asText());
            }
            for ( JsonNode layer : manifest.at("/layers") ) {
                digests.add(layer.at("/digest").asText());
            }
        } else if ( manifest.at("/fsLayers").isArray() ) {
            for ( JsonNode layer : manifest.at("/fsLayers") ) {
                digests.add(layer.at("/blobSum").asText());
            }
        }
        return digests;
    }
}
