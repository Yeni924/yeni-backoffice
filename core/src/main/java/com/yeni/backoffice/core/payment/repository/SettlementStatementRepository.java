package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SettlementStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementStatementRepository extends JpaRepository<SettlementStatement, Long> {

    Optional<SettlementStatement> findBySettlementDateAndMid(LocalDate settlementDate, String mid);

    List<SettlementStatement> findBySettlementDateBetweenOrderBySettlementDateDesc(LocalDate startDate, LocalDate endDate);
}
