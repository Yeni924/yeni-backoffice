package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.config.InicisStdPayProperties;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementBatchRunRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementStatementResponse;
import com.yeni.backoffice.core.payment.entity.PgFeePolicy;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementBatchLog;
import com.yeni.backoffice.core.payment.entity.SettlementDetail;
import com.yeni.backoffice.core.payment.entity.SettlementFeeDetail;
import com.yeni.backoffice.core.payment.entity.SettlementLog;
import com.yeni.backoffice.core.payment.entity.SettlementStatement;
import com.yeni.backoffice.core.payment.enums.BatchStatus;
import com.yeni.backoffice.core.payment.enums.SettlementStatus;
import com.yeni.backoffice.core.payment.repository.PgFeePolicyRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementBatchLogRepository;
import com.yeni.backoffice.core.payment.repository.SettlementDetailRepository;
import com.yeni.backoffice.core.payment.repository.SettlementFeeDetailRepository;
import com.yeni.backoffice.core.payment.repository.SettlementLogRepository;
import com.yeni.backoffice.core.payment.repository.SettlementStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementOperationService {

    private final InicisStdPayProperties inicisProperties;
    private final SalesTransactionRepository salesRepository;
    private final PgFeePolicyRepository feePolicyRepository;
    private final SettlementStatementRepository settlementStatementRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final SettlementFeeDetailRepository settlementFeeDetailRepository;
    private final SettlementBatchLogRepository batchLogRepository;
    private final SettlementLogRepository settlementLogRepository;

    public SettlementOperationService(
            InicisStdPayProperties inicisProperties,
            SalesTransactionRepository salesRepository,
            PgFeePolicyRepository feePolicyRepository,
            SettlementStatementRepository settlementStatementRepository,
            SettlementDetailRepository settlementDetailRepository,
            SettlementFeeDetailRepository settlementFeeDetailRepository,
            SettlementBatchLogRepository batchLogRepository,
            SettlementLogRepository settlementLogRepository) {
        this.inicisProperties = inicisProperties;
        this.salesRepository = salesRepository;
        this.feePolicyRepository = feePolicyRepository;
        this.settlementStatementRepository = settlementStatementRepository;
        this.settlementDetailRepository = settlementDetailRepository;
        this.settlementFeeDetailRepository = settlementFeeDetailRepository;
        this.batchLogRepository = batchLogRepository;
        this.settlementLogRepository = settlementLogRepository;
    }

    @Transactional
    public SettlementStatementResponse runDailySettlement(SettlementBatchRunRequest request) {
        LocalDate targetDate = request == null || request.targetDate() == null ? LocalDate.now().minusDays(1) : request.targetDate();
        settlementStatementRepository.findBySettlementDateAndMid(targetDate, inicisProperties.getMid())
                .ifPresent(statement -> {
                    throw new IllegalArgumentException("Settlement statement already exists.");
                });

        List<SalesTransaction> sales = salesRepository.findByBusinessDateAndSettlementIncludedYnFalseOrderByIdAsc(targetDate);
        SettlementBatchLog batchLog = batchLogRepository.save(SettlementBatchLog.builder()
                .targetDate(targetDate)
                .batchStatus(BatchStatus.RUNNING)
                .targetCount(sales.size())
                .successCount(0)
                .failureCount(0)
                .startedAt(LocalDateTime.now())
                .build());

        try {
            PgFeePolicy feePolicy = findFeePolicy(targetDate);
            BigDecimal grossAmount = sales.stream()
                    .map(SalesTransaction::getSaleAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal feeAmount = calculateFee(grossAmount.abs(), feePolicy.getFeeRate());
            BigDecimal vatAmount = feeAmount.divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            BigDecimal netAmount = grossAmount.subtract(feeAmount).subtract(vatAmount);

            SettlementStatement statement = settlementStatementRepository.save(SettlementStatement.builder()
                    .settlementDate(targetDate)
                    .pgCompany("INICIS")
                    .mid(inicisProperties.getMid())
                    .grossAmount(grossAmount)
                    .feeAmount(feeAmount)
                    .vatAmount(vatAmount)
                    .netAmount(netAmount)
                    .settlementStatus(SettlementStatus.DRAFT)
                    .build());

            for (SalesTransaction sale : sales) {
                BigDecimal saleFee = calculateFee(sale.getSaleAmount().abs(), feePolicy.getFeeRate());
                settlementDetailRepository.save(SettlementDetail.builder()
                        .settlementStatementId(statement.getId())
                        .salesId(sale.getId())
                        .saleType(sale.getSaleType().name())
                        .saleAmount(sale.getSaleAmount())
                        .feeAmount(saleFee)
                        .netAmount(sale.getSaleAmount().subtract(saleFee))
                        .build());
                sale.markIncludedInSettlement();
            }

            settlementFeeDetailRepository.save(SettlementFeeDetail.builder()
                    .settlementStatementId(statement.getId())
                    .feePolicyId(feePolicy.getId())
                    .feeRate(feePolicy.getFeeRate())
                    .feeAmount(feeAmount)
                    .vatAmount(vatAmount)
                    .build());

            batchLog.complete(sales.size(), 0);
            saveSettlementLog(statement.getId(), "BATCH_RUN", "SUCCESS", "정산 초안 생성 완료");
            return SettlementStatementResponse.from(statement);
        } catch (RuntimeException e) {
            batchLog.fail(e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<SettlementStatementResponse> getStatements(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return settlementStatementRepository.findBySettlementDateBetweenOrderBySettlementDateDesc(start, end).stream()
                .map(SettlementStatementResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SettlementDetailPageResponse getStatement(Long statementId) {
        SettlementStatement statement = settlementStatementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement statement not found."));
        List<SettlementDetailResponse> details = settlementDetailRepository.findBySettlementStatementIdOrderByIdAsc(statementId).stream()
                .map(SettlementDetailResponse::from)
                .collect(Collectors.toList());
        return new SettlementDetailPageResponse(SettlementStatementResponse.from(statement), details);
    }

    @Transactional
    public SettlementStatementResponse confirmStatement(Long statementId) {
        SettlementStatement statement = settlementStatementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement statement not found."));
        if (!SettlementStatus.DRAFT.equals(statement.getSettlementStatus())) {
            throw new IllegalArgumentException("Only draft settlement can be confirmed.");
        }
        settlementDetailRepository.findBySettlementStatementIdOrderByIdAsc(statementId)
                .forEach(detail -> salesRepository.findById(detail.getSalesId()).ifPresent(SalesTransaction::markSettled));
        statement.confirm();
        saveSettlementLog(statementId, "CONFIRM", "SUCCESS", "정산 확정 완료");
        return SettlementStatementResponse.from(statement);
    }

    @Transactional
    public SettlementStatementResponse markPaid(Long statementId) {
        SettlementStatement statement = settlementStatementRepository.findById(statementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement statement not found."));
        if (!SettlementStatus.CONFIRMED.equals(statement.getSettlementStatus())) {
            throw new IllegalArgumentException("Only confirmed settlement can be paid.");
        }
        statement.markPaid();
        saveSettlementLog(statementId, "PAY", "SUCCESS", "정산 지급 처리 완료");
        return SettlementStatementResponse.from(statement);
    }

    private PgFeePolicy findFeePolicy(LocalDate targetDate) {
        return feePolicyRepository
                .findFirstByPgCompanyAndMidAndPaymentMethodAndUseYnTrueAndEffectiveStartDateLessThanEqualAndEffectiveEndDateGreaterThanEqual(
                        "INICIS",
                        inicisProperties.getMid(),
                        PaymentOperationService.paymentMethod(),
                        targetDate,
                        targetDate
                )
                .orElseGet(() -> feePolicyRepository.save(PgFeePolicy.builder()
                        .pgCompany("INICIS")
                        .mid(inicisProperties.getMid())
                        .paymentMethod(PaymentOperationService.paymentMethod())
                        .feeRate(new BigDecimal("0.0250"))
                        .effectiveStartDate(LocalDate.of(2020, 1, 1))
                        .effectiveEndDate(LocalDate.of(2099, 12, 31))
                        .useYn(true)
                        .build()));
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal feeRate) {
        return amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
    }

    private void saveSettlementLog(Long statementId, String actionType, String resultStatus, String message) {
        settlementLogRepository.save(SettlementLog.builder()
                .settlementStatementId(statementId)
                .actionType(actionType)
                .resultStatus(resultStatus)
                .message(message)
                .loggedAt(LocalDateTime.now())
                .build());
    }
}
