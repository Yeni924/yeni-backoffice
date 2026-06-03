package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PaymentCancel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {

    Optional<PaymentCancel> findByCancelRequestKey(String cancelRequestKey);
}
