package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {

    List<SettlementDetail> findBySettlementStatementIdOrderByIdAsc(Long settlementStatementId);
}
