package com.yeni.backoffice.core.admin.auth.repository;

import com.yeni.backoffice.core.admin.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByLoginIdAndIsDeletedFalse(String loginId);
    boolean existsByLoginId(String loginId);
}
