package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementBatchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchLogRepository extends JpaRepository<SettlementBatchLog, Long> {
}
