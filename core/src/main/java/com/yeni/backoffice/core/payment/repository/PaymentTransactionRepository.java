package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderNo(String orderNo);

    Optional<PaymentTransaction> findByTid(String tid);
}
