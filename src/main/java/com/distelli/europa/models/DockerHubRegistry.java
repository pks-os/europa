package com.distelli.europa.models;

import com.distelli.gcr.GcrClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DockerHubRegistry extends RemoteRegistry {
    public interface Factory {
        DockerHubRegistry create(ContainerRepo repo, Boolean isPush, String crossBlobMountFrom);
        DockerHubRegistry create(ContainerRepo repo, Boolean isPush);
    }

    private static final URI ENDPOINT_URI = URI.create("https://index.docker.io/");
    private static final URI AUTH_URI = URI.create("https://auth.docker.io/");
    private static final ObjectMapper OM = new ObjectMapper();

    @Inject
    private ConnectionPool _connectionPool;

    private boolean isPush;
    private String crossBlobMountFrom;

    @AssistedInject
    public DockerHubRegistry(@Assisted ContainerRepo repo,
                             @Assisted Boolean isPush,
                             @Assisted String crossBlobMountFrom) throws IOException {
        super(repo);
        this.isPush = isPush;
        this.crossBlobMountFrom = crossBlobMountFrom;
    }

    @AssistedInject
    public DockerHubRegistry(@Assisted ContainerRepo repo,
                             @Assisted Boolean isPush) throws IOException {
        super(repo);
        this.isPush = isPush;
        this.crossBlobMountFrom = null;
    }

    protected GcrClient createClient() throws IOException {
        String token = getToken();
        return _gcrClientBuilder.gcrCredentials(() -> "Bearer " + token)
            .endpoint(ENDPOINT_URI)
            .build();
    }

    private String getToken() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(_connectionPool)
            .build();
        StringBuilder scope = new StringBuilder();
        scope.append(String.format("repository:%s:pull", getRepo().getName()));
        if (isPush) {
            scope.append(",push");
            if (crossBlobMountFrom != null) {
                scope.append(String.format(" repository:%s:pull", crossBlobMountFrom));
            }
        }
        Request req = new Request.Builder()
            .get()
            .header("Authorization",
                    "Basic " +
                    Base64.getEncoder()
                        .encodeToString((getCred().getUsername() + ":" + getCred().getPassword()).getBytes(UTF_8)))
            .url(HttpUrl.get(AUTH_URI).newBuilder()
                     .addPathSegments("/token")
                     .addQueryParameter("service", "registry.docker.io")
                     .addQueryParameter("scope", scope.toString())
                     .build())
            .build();
        try ( Response res = client.newCall(req).execute() ) {
            if ( res.code() / 100 != 2 ) {
                throw new HttpError(res.code(), res.body().string());
            }
            JsonNode json = OM.readTree(res.body().byteStream());
            return json.at("/token").asText();
        }
    }
}
