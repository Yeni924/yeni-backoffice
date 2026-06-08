package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFollowUpService {

    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;

    public PaymentFollowUpService(
            ExternalSendRequestRepository externalSendRequestRepository,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository) {
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
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

    @Transactional
    public boolean createRecoveryTask(Long paymentId, Long cancelId, RecoveryType recoveryType, String taskKey, String lastErrorMessage) {
        return createRecoveryTask(paymentId, cancelId, null, null, null, recoveryType, taskKey, lastErrorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            recoveryTaskRepository.save(PaymentRecoveryTask.builder()
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
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Transactional
    public void markRecoverySuccess(String taskKey) {
        recoveryTaskRepository.findByTaskKey(taskKey)
                .ifPresent(PaymentRecoveryTask::markSuccess);
    }
}
