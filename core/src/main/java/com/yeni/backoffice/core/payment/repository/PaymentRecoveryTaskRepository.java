package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRecoveryTaskRepository extends JpaRepository<PaymentRecoveryTask, Long> {

    Optional<PaymentRecoveryTask> findByTaskKey(String taskKey);

    List<PaymentRecoveryTask> findByPaymentIdOrderByIdAsc(Long paymentId);

    List<PaymentRecoveryTask> findByCancelIdOrderByIdAsc(Long cancelId);
}
