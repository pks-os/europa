package com.distelli.europa.registry;

import com.distelli.europa.handlers.*;
import com.distelli.europa.security.HandlerAccessAuthorization;
import com.distelli.europa.security.HandlerAccessRules;
import com.distelli.europa.security.Permission;
import com.distelli.webserver.RequestHandler;
import lombok.extern.log4j.Log4j;

import java.util.List;

@Log4j
public class RegistryAccessAuthorization implements HandlerAccessAuthorization{
    private static HandlerAccessRules ACCESS_RULES = new HandlerAccessRules();

    static {
        ACCESS_RULES.addAccessRule(RegistryVersionCheck.class, Permission.NONE);

        ACCESS_RULES.addAccessRule(RegistryManifestPush.class, Permission.PUSH);
        ACCESS_RULES.addAccessRule(RegistryManifestPull.class, Permission.PULL);
        ACCESS_RULES.addAccessRule(RegistryManifestExists.class, Permission.PULL);
        ACCESS_RULES.addAccessRule(RegistryManifestDelete.class, Permission.PUSH);

        ACCESS_RULES.addAccessRule(RegistryLayerPull.class, Permission.PULL);
        ACCESS_RULES.addAccessRule(RegistryLayerExists.class, Permission.PULL);
        ACCESS_RULES.addAccessRule(RegistryLayerExists.class, Permission.PUSH); // used by both push and pull
        ACCESS_RULES.addAccessRule(RegistryLayerDelete.class, Permission.PUSH);

        ACCESS_RULES.addAccessRule(RegistryLayerUploadBegin.class, Permission.PUSH);
        ACCESS_RULES.addAccessRule(RegistryLayerUploadFinish.class, Permission.PUSH);
        ACCESS_RULES.addAccessRule(RegistryLayerUploadChunk.class, Permission.PUSH);
        ACCESS_RULES.addAccessRule(RegistryLayerUploadProgress.class, Permission.PUSH);
        ACCESS_RULES.addAccessRule(RegistryLayerUploadCancel.class, Permission.PUSH);

        ACCESS_RULES.addAccessRule(RegistryTagList.class, Permission.PULL);

        ACCESS_RULES.addAccessRule(RegistryCatalog.class, Permission.NONE);
        ACCESS_RULES.addAccessRule(RegistryTokenHandler.class, Permission.NONE);
        ACCESS_RULES.addAccessRule(RegistryDefault.class, Permission.NONE);
    }

    @Override
    public boolean checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler) {
        // If we're authenticated at all, we have full permissions on repos
        if (requesterDomain == null || requesterDomain.isEmpty()) {
            return false;
        }
        return ACCESS_RULES.hasAccess(handler, Permission.FULLCONTROL);
    }

    @Override
    public boolean checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler, Object obj) {
        return checkAuthorization(requesterDomain, handler);
    }

    @Override
    public <T> List<T> checkAuthorization(String requesterDomain, Class<? extends RequestHandler> handler, List<T> list) {
        return (checkAuthorization(requesterDomain, handler)) ? list : null;
    }
}
