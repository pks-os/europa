package com.distelli.europa.security;

import com.distelli.webserver.RequestHandler;

import java.util.List;

public interface HandlerAccessAuthorization {
    boolean checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler);
    boolean checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler, Object obj);
    <T> List<T> checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler, List<T> list);
}
