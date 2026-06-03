package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    List<SalesTransaction> findByBusinessDateAndSettlementIncludedYnFalseOrderByIdAsc(LocalDate businessDate);

    List<SalesTransaction> findByBusinessDateBetweenOrderByOccurredAtDesc(LocalDate startDate, LocalDate endDate);
}
