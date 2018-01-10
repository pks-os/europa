package com.distelli.europa.filters;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class HttpAlwaysAllowedPaths {

    private static final Set<String> HTTP_ALWAYS_ALLOWED_PATHS = new HashSet<String>();

    static {
        HTTP_ALWAYS_ALLOWED_PATHS.add("/healthz");
    }

    public HttpAlwaysAllowedPaths() {

    }

    public boolean isHttpAlwaysAllowedPath(String path) {
        if (HTTP_ALWAYS_ALLOWED_PATHS.contains(path)) {
            return true;
        }
        return false;
    }
}
