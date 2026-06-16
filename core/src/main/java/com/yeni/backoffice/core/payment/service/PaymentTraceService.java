package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.AlimtalkQueueResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ExternalSendResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentCancelTraceResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentTraceResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgLogResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailResponse;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentCancelRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.PgApiLogRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentTraceService {

    private final PaymentTransactionRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final SalesTransactionRepository salesRepository;
    private final ExternalSendRequestRepository externalSendRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final PgApiLogRepository pgApiLogRepository;

    public PaymentTraceService(
            PaymentTransactionRepository paymentRepository,
            PaymentCancelRepository cancelRepository,
            SalesTransactionRepository salesRepository,
            ExternalSendRequestRepository externalSendRepository,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            SettlementDetailRepository settlementDetailRepository,
            PgApiLogRepository pgApiLogRepository) {
        this.paymentRepository = paymentRepository;
        this.cancelRepository = cancelRepository;
        this.salesRepository = salesRepository;
        this.externalSendRepository = externalSendRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.settlementDetailRepository = settlementDetailRepository;
        this.pgApiLogRepository = pgApiLogRepository;
    }

    @Transactional(readOnly = true)
    public PaymentTraceResponse getTrace(Long paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        List<SalesTransaction> sales = salesRepository.findByPaymentIdOrderByIdAsc(paymentId);

        return new PaymentTraceResponse(
                PaymentResponse.from(payment),
                cancelRepository.findByPaymentIdOrderByIdAsc(paymentId).stream()
                        .map(PaymentCancelTraceResponse::from)
                        .toList(),
                sales.stream().map(SalesResponse::from).toList(),
                sales.stream()
                        .flatMap(item -> externalSendRepository.findBySalesIdOrderByIdAsc(item.getId()).stream())
                        .map(ExternalSendResponse::from)
                        .toList(),
                sales.stream()
                        .flatMap(item -> alimtalkQueueRepository.findBySalesIdOrderByIdAsc(item.getId()).stream())
                        .map(AlimtalkQueueResponse::from)
                        .toList(),
                recoveryTaskRepository.findByPaymentIdOrderByIdAsc(paymentId).stream()
                        .map(RecoveryTaskResponse::from)
                        .toList(),
                sales.stream()
                        .flatMap(item -> settlementDetailRepository.findBySalesIdOrderByIdAsc(item.getId()).stream())
                        .map(SettlementDetailResponse::from)
                        .toList(),
                pgApiLogRepository.findByOrderNoOrderByLoggedAtDesc(payment.getOrderNo()).stream()
                        .map(PgLogResponse::from)
                        .toList()
        );
    }
}
