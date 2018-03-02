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
import javax.inject.Provider;
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

    @AssistedInject
    public DockerHubRegistry(DockerHubGcrClientGenerator.Factory dockerHubGcrClientGeneratorFactory,
                             @Assisted ContainerRepo repo,
                             @Assisted Boolean isPush,
                             @Assisted String crossBlobMountFrom
                            ) throws IOException {
        super(repo, dockerHubGcrClientGeneratorFactory.create(isPush, crossBlobMountFrom));
    }

    @AssistedInject
    public DockerHubRegistry(DockerHubGcrClientGenerator.Factory dockerHubGcrClientGeneratorFactory,
                             @Assisted ContainerRepo repo,
                             @Assisted Boolean isPush
                             ) throws IOException {
        super(repo, dockerHubGcrClientGeneratorFactory.create(isPush));
    }

    public static class DockerHubGcrClientGenerator implements GcrClientGenerator {
        public interface Factory {
            DockerHubGcrClientGenerator create(Boolean isPush, String crossBlobMountFrom);
            DockerHubGcrClientGenerator create(Boolean isPush);
        }

        @Inject
        private ConnectionPool _connectionPool;

        private boolean isPush;
        private String crossBlobMountFrom;

        @AssistedInject
        public DockerHubGcrClientGenerator(@Assisted Boolean isPush,
                                           @Assisted String crossBlobMountFrom) {
            this.isPush = isPush;
            this.crossBlobMountFrom = crossBlobMountFrom;
        }

        @AssistedInject
        public DockerHubGcrClientGenerator(@Assisted Boolean isPush) {
            this.isPush = isPush;
            this.crossBlobMountFrom = crossBlobMountFrom;
        }

        @Override
        public GcrClient createClient(Provider<GcrClient.Builder> gcrClientBuilderProvider, ContainerRepo repo, RegistryCred cred) throws IOException {
            String token = getToken(repo.getName(), cred, isPush, crossBlobMountFrom);
            return gcrClientBuilderProvider.get()
                .gcrCredentials(() -> "Bearer " + token)
                .endpoint(ENDPOINT_URI)
                .build();
        }

        private String getToken(String repoName, RegistryCred cred, boolean isPush, String crossBlobMountFrom) throws IOException {
            if (_connectionPool == null) {
                throw new IllegalStateException("Injector.injectMembers(this) has not been called");
            }
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
