package com.yeni.backoffice.core.admin.auth.service;

import com.yeni.backoffice.core.admin.auth.dto.AdminLoginResult;
import com.yeni.backoffice.core.admin.auth.dto.LoginAdminDto;
import com.yeni.backoffice.core.admin.auth.entity.AdminUser;
import com.yeni.backoffice.core.admin.auth.repository.AdminUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Transactional
    public AdminLoginResult login(String loginId, String password) {
        if (!StringUtils.hasText(loginId) || !StringUtils.hasText(password)) {
            return AdminLoginResult.fail("Please enter ID and password.");
        }

        return adminUserRepository.findByLoginIdAndIsDeletedFalse(loginId.trim())
                .map(adminUser -> authenticate(adminUser, password))
                .orElseGet(() -> AdminLoginResult.fail("Invalid ID or password."));
    }

    private AdminLoginResult authenticate(AdminUser adminUser, String password) {
        if (!adminUser.canLogin()) {
            return AdminLoginResult.fail("This account cannot sign in.");
        }

        if (!passwordEncoder.matches(password, adminUser.getPasswordHash())) {
            adminUser.recordLoginFailure();
            return AdminLoginResult.fail("Invalid ID or password.");
        }

        adminUser.recordLoginSuccess();
        return AdminLoginResult.success(new LoginAdminDto(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getAdminName(),
                adminUser.getRole()
        ));
    }
}
