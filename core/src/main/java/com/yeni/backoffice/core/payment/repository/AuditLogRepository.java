package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
