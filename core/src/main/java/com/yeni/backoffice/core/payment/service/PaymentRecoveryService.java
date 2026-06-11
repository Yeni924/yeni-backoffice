package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRecoveryService.class);

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final RecoveryTaskRecorder recoveryTaskRecorder;

    public PaymentRecoveryService(
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            RecoveryTaskRecorder recoveryTaskRecorder) {
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.recoveryTaskRecorder = recoveryTaskRecorder;
    }

    public boolean createRecoveryTask(Long paymentId, Long cancelId, RecoveryType recoveryType, String taskKey, String lastErrorMessage) {
        return createRecoveryTask(paymentId, cancelId, null, null, null, recoveryType, taskKey, lastErrorMessage);
    }

    public boolean createRecoveryTask(
            Long paymentId,
            Long cancelId,
            String orderNo,
            String tid,
            String idempotencyKey,
            RecoveryType recoveryType,
            String taskKey,
            String lastErrorMessage) {
        if (recoveryTaskRepository.findByTaskKey(taskKey).isPresent()) {
            return false;
        }
        try {
            recoveryTaskRecorder.record(paymentId, cancelId, orderNo, tid, idempotencyKey, recoveryType, taskKey, lastErrorMessage);
            log.warn("RecoveryTask recorded in an independent transaction. type={}, taskKey={}, orderNo={}, tid={}",
                    recoveryType, taskKey, orderNo, tid);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.info("RecoveryTask already exists. type={}, taskKey={}", recoveryType, taskKey);
            return false;
        }
    }

    @Transactional
    public void markRecoverySuccess(String taskKey) {
        recoveryTaskRepository.findByTaskKey(taskKey)
                .ifPresent(PaymentRecoveryTask::markSuccess);
    }
}
