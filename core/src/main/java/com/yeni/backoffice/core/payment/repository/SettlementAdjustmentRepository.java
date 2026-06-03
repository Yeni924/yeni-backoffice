package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementAdjustmentRepository extends JpaRepository<SettlementAdjustment, Long> {

    List<SettlementAdjustment> findBySalesIdOrderByIdDesc(Long salesId);
}
