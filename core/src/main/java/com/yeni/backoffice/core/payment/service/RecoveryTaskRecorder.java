package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryTaskRecorder {

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;

    public RecoveryTaskRecorder(PaymentRecoveryTaskRepository recoveryTaskRepository) {
        this.recoveryTaskRepository = recoveryTaskRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long paymentId,
            Long cancelId,
            String orderNo,
            String tid,
            String idempotencyKey,
            RecoveryType recoveryType,
            String taskKey,
            String lastErrorMessage) {
        recoveryTaskRepository.saveAndFlush(PaymentRecoveryTask.builder()
                .taskKey(taskKey)
                .paymentId(paymentId)
                .cancelId(cancelId)
                .orderNo(orderNo)
                .tid(tid)
                .idempotencyKey(idempotencyKey)
                .recoveryType(recoveryType)
                .status(RecoveryStatus.READY)
                .retryCount(0)
                .maxRetryCount(5)
                .lastErrorMessage(lastErrorMessage)
                .build());
    }
}
