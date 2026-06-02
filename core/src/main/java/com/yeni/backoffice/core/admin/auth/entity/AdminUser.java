package com.yeni.backoffice.core.admin.auth.entity;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "admin_user")
public class AdminUser extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String adminName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean useYn = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean lockedYn = false;

    @Column
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer failedLoginCount = 0;

    public boolean canLogin() {
        return Boolean.TRUE.equals(useYn) && !Boolean.TRUE.equals(lockedYn) && !Boolean.TRUE.equals(getIsDeleted());
    }

    public void recordLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.failedLoginCount = 0;
    }

    public void recordLoginFailure() {
        this.failedLoginCount = this.failedLoginCount == null ? 1 : this.failedLoginCount + 1;
    }
}
