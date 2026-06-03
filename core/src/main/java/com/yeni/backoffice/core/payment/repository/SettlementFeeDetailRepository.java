package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementFeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementFeeDetailRepository extends JpaRepository<SettlementFeeDetail, Long> {

    List<SettlementFeeDetail> findBySettlementStatementIdOrderByIdAsc(Long settlementStatementId);
}
