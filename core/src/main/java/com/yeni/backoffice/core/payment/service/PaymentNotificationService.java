package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentNotificationService {

    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;

    public PaymentNotificationService(
            ExternalSendRequestRepository externalSendRequestRepository,
            AlimtalkQueueRepository alimtalkQueueRepository) {
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
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
}
