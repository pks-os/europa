package com.distelli.europa.models;

import com.distelli.gcr.GcrClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DockerHubRegistry extends RemoteRegistry {
    private static final URI ENDPOINT_URI = URI.create("https://index.docker.io/");
    private static final URI AUTH_URI = URI.create("https://auth.docker.io/");
    private static final ObjectMapper OM = new ObjectMapper();

    public DockerHubRegistry(ContainerRepo repo) throws IOException {
        super(repo, new DockerHubGcrClientGenerator(false, null));
    }

    public DockerHubRegistry(ContainerRepo repo, boolean isPush, String crossBlobMountFrom) throws IOException {
        super(repo, new DockerHubGcrClientGenerator(isPush, crossBlobMountFrom));
    }

    private static class DockerHubGcrClientGenerator implements GcrClientGenerator {
        @Inject
        private ConnectionPool _connectionPool;

        private boolean isPush;
        private String crossBlobMountFrom;

        public DockerHubGcrClientGenerator(boolean isPush, String crossBlobMountFrom) {
            this.isPush = isPush;
            this.crossBlobMountFrom = crossBlobMountFrom;
        }

        @Override
        public GcrClient getClient(Provider<GcrClient.Builder> gcrClientBuilderProvider, ContainerRepo repo, RegistryCred cred) throws IOException {
            String token = getToken(repo.getName(), cred, isPush, crossBlobMountFrom);
            return gcrClientBuilderProvider.get()
                .gcrCredentials(() -> "Bearer " + token)
                .endpoint(ENDPOINT_URI)
                .build();
        }

        private String getToken(String repoName, RegistryCred cred, boolean isPush, String crossBlobMountFrom) throws IOException {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(_connectionPool)
                .build();
            String scope = null;
            if ( ! isPush ) {
                scope = "repository:"+repoName+":pull";
            } else if ( null == crossBlobMountFrom ) {
                scope = "repository:"+repoName+":pull,push";
            } else {
                scope = "repository:"+repoName+":pull,push repository:"+crossBlobMountFrom+":pull";
            }
            Request req = new Request.Builder()
                .get()
                .header("Authorization",
                        "Basic " +
                            Base64.getEncoder()
                                .encodeToString((cred.getUsername() + ":" + cred.getPassword()).getBytes(UTF_8)))
                .url(HttpUrl.get(AUTH_URI).newBuilder()
                         .addPathSegments("/token")
                         .addQueryParameter("service", "registry.docker.io")
                         .addQueryParameter("scope", scope)
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
}
