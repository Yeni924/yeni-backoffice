package com.yeni.backoffice.core.admin.auth;

public enum AdminRole {
    USER,
    ADMIN;

    public boolean canAccess(AdminRole requiredRole) {
        if (requiredRole == null || requiredRole == USER) {
            return true;
        }
        return this == ADMIN;
    }
}
