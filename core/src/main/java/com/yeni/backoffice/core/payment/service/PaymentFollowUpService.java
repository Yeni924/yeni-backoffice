package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFollowUpService {

    private static final Logger log = LoggerFactory.getLogger(PaymentFollowUpService.class);

    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final RecoveryTaskRecorder recoveryTaskRecorder;

    public PaymentFollowUpService(
            ExternalSendRequestRepository externalSendRequestRepository,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            RecoveryTaskRecorder recoveryTaskRecorder) {
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.recoveryTaskRecorder = recoveryTaskRecorder;
    }

    @Transactional
    public boolean createExternalSendRequest(SalesTransaction sales, String requestKey) {
        if (externalSendRequestRepository.findByRequestKey(requestKey).isPresent()) {
            return false;
        }
        try {
            externalSendRequestRepository.save(ExternalSendRequest.builder()
                    .salesId(sales.getId())
                    .requestKey(requestKey)
                    .targetSystem("SALES_OPERATION_MOCK")
                    .sendStatus(ExternalSendStatus.READY)
                    .retryCount(0)
                    .build());
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Transactional
    public boolean createAlimtalkQueue(PaymentTransaction payment, SalesTransaction sales, String messageKey, String eventType) {
        if (alimtalkQueueRepository.findByMessageKey(messageKey).isPresent()) {
            return false;
        }
        try {
            alimtalkQueueRepository.save(AlimtalkQueue.builder()
                    .paymentId(payment.getId())
                    .salesId(sales.getId())
                    .messageKey(messageKey)
                    .eventType(eventType)
                    .status(AlimtalkStatus.READY)
                    .retryCount(0)
                    .build());
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
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
