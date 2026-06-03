package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PgApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PgApiLogRepository extends JpaRepository<PgApiLog, Long> {

    List<PgApiLog> findByPaymentIdOrderByLoggedAtDesc(Long paymentId);

    List<PgApiLog> findByOrderNoOrderByLoggedAtDesc(String orderNo);
}
