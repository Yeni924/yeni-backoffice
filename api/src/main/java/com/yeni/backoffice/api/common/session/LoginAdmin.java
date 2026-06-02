package com.yeni.backoffice.api.common.session;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.auth.dto.LoginAdminDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginAdmin {
    private Long adminUserId;
    private String loginId;
    private String adminName;
    private AdminRole role;

    public static LoginAdmin from(LoginAdminDto dto) {
        return new LoginAdmin(dto.getAdminUserId(), dto.getLoginId(), dto.getAdminName(), dto.getRole());
    }

    public boolean isAdmin() {
        return role == AdminRole.ADMIN;
    }
}
