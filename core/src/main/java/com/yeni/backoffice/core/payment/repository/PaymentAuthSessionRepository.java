package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PaymentAuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAuthSessionRepository extends JpaRepository<PaymentAuthSession, Long> {

    Optional<PaymentAuthSession> findByOrderNo(String orderNo);

    Optional<PaymentAuthSession> findByAuthToken(String authToken);
}
