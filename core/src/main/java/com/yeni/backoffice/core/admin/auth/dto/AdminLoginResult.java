package com.yeni.backoffice.core.admin.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminLoginResult {
    private boolean success;
    private String message;
    private LoginAdminDto loginAdmin;

    public static AdminLoginResult success(LoginAdminDto loginAdmin) {
        return new AdminLoginResult(true, "Login succeeded.", loginAdmin);
    }

    public static AdminLoginResult fail(String message) {
        return new AdminLoginResult(false, message, null);
    }
}
