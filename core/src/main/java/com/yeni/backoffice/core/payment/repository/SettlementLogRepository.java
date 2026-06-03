package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementLogRepository extends JpaRepository<SettlementLog, Long> {

    List<SettlementLog> findBySettlementStatementIdOrderByLoggedAtDesc(Long settlementStatementId);
}
