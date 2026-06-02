package com.yeni.backoffice.core.admin.auth.init;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.auth.entity.AdminUser;
import com.yeni.backoffice.core.admin.auth.repository.AdminUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserDataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminUserDataInitializer(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.existsByLoginId("test")) {
            return;
        }

        AdminUser testAdmin = AdminUser.builder()
                .loginId("test")
                .passwordHash(passwordEncoder.encode("1234"))
                .adminName("Test Admin")
                .role(AdminRole.ADMIN)
                .useYn(true)
                .lockedYn(false)
                .failedLoginCount(0)
                .build();

        adminUserRepository.save(testAdmin);
    }
}
