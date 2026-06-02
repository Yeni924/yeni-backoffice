package com.yeni.backoffice.core.admin.auth.dto;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginAdminDto {
    private Long adminUserId;
    private String loginId;
    private String adminName;
    private AdminRole role;

    public boolean isAdmin() {
        return role == AdminRole.ADMIN;
    }
}
