package com.distelli.europa.security;

import com.distelli.webserver.RequestHandler;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HandlerAccessRules {
    private Set<HandlerAccessRule> accessRules = new HashSet<>();

    public void addAccessRule(Class<? extends RequestHandler> handler, Permission sufficientPermission) {
        HandlerAccessRule newRule = new HandlerAccessRule(handler, sufficientPermission);
        accessRules.add(newRule);
    }

    public void addAccessRule(Class<? extends RequestHandler> handler, Permission[] sufficientPermissions) {
        addAccessRule(handler, Arrays.asList(sufficientPermissions));
    }

    public void addAccessRule(Class<? extends RequestHandler> handler, List<Permission> sufficientPermissions) {
        sufficientPermissions.forEach((permission -> addAccessRule(handler, permission)));
    }

    public boolean hasAccess(Class<? extends RequestHandler> handler, Permission permission) {
        return hasAccess(handler, Arrays.asList(permission));
    }

    public boolean hasAccess(Class<? extends RequestHandler> handler, List<Permission> permissions) {
        if (accessRules.contains(new HandlerAccessRule(handler, Permission.NONE)) || permissions.contains(Permission.FULLCONTROL)) {
            return true;
        }
        return (permissions.stream()
            .map((permission) -> new HandlerAccessRule(handler, permission))
            .filter(accessRules::contains)
            .count() > 0);
    }

    private class HandlerAccessRule {
        @Getter
        private final Class<? extends RequestHandler> handler;
        @Getter
        private final Permission permission;

        public HandlerAccessRule(Class<? extends RequestHandler> handler, Permission requiredPermission) {
            this.handler = handler;
            this.permission = requiredPermission;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HandlerAccessRule) {
                HandlerAccessRule newObj = (HandlerAccessRule)obj;
                return (handler == newObj.getHandler() && permission == newObj.getPermission());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(handler, permission);
        }
    }
}
