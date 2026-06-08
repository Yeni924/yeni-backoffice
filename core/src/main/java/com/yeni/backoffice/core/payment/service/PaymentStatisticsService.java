package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.OperationSummaryResponse;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PaymentStatisticsService {

    private final PaymentTransactionRepository paymentRepository;
    private final SalesTransactionRepository salesRepository;
    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;

    public PaymentStatisticsService(
            PaymentTransactionRepository paymentRepository,
            SalesTransactionRepository salesRepository,
            ExternalSendRequestRepository externalSendRequestRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            AlimtalkQueueRepository alimtalkQueueRepository) {
        this.paymentRepository = paymentRepository;
        this.salesRepository = salesRepository;
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
    }

    @Transactional(readOnly = true)
    public OperationSummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        BigDecimal approvedAmount = paymentRepository.findAll().stream()
                .filter(payment -> !payment.getApprovedAt().toLocalDate().isBefore(start)
                        && !payment.getApprovedAt().toLocalDate().isAfter(end))
                .map(PaymentTransaction::getApprovedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salesAmount = salesRepository.findByBusinessDateBetweenOrderByOccurredAtDesc(start, end).stream()
                .map(SalesTransaction::getSaleAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OperationSummaryResponse(
                start,
                end,
                paymentRepository.count(),
                approvedAmount,
                salesRepository.findByBusinessDateBetweenOrderByOccurredAtDesc(start, end).size(),
                salesAmount,
                externalSendRequestRepository.findBySendStatusOrderByIdAsc(ExternalSendStatus.READY).size(),
                externalSendRequestRepository.findBySendStatusOrderByIdAsc(ExternalSendStatus.FAILED).size(),
                recoveryTaskRepository.count(),
                alimtalkQueueRepository.findByStatusOrderByIdAsc(AlimtalkStatus.READY).size(),
                alimtalkQueueRepository.findByStatusOrderByIdAsc(AlimtalkStatus.FAILED).size()
        );
    }
}
