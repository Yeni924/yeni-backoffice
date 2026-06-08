package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.AlimtalkQueueResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ExternalSendResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesAdjustmentRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerLinksResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerSummaryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailResponse;
import com.yeni.backoffice.core.payment.entity.AuditLog;
import com.yeni.backoffice.core.payment.entity.PaymentCancel;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementAdjustment;
import com.yeni.backoffice.core.payment.enums.LedgerStatus;
import com.yeni.backoffice.core.payment.enums.SaleStatus;
import com.yeni.backoffice.core.payment.enums.SaleType;
import com.yeni.backoffice.core.payment.enums.SalesSettlementStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.AuditLogRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentCancelRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementAdjustmentRepository;
import com.yeni.backoffice.core.payment.repository.SettlementDetailRepository;
import com.yeni.backoffice.core.payment.support.PaymentDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesLedgerService {

    private final SalesTransactionRepository salesRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final SettlementAdjustmentRepository adjustmentRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final AuditLogRepository auditLogRepository;

    public SalesLedgerService(
            SalesTransactionRepository salesRepository,
            PaymentTransactionRepository paymentRepository,
            PaymentCancelRepository cancelRepository,
            ExternalSendRequestRepository externalSendRequestRepository,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            SettlementAdjustmentRepository adjustmentRepository,
            SettlementDetailRepository settlementDetailRepository,
            AuditLogRepository auditLogRepository) {
        this.salesRepository = salesRepository;
        this.paymentRepository = paymentRepository;
        this.cancelRepository = cancelRepository;
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.settlementDetailRepository = settlementDetailRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public SalesTransaction createSales(PaymentTransaction payment, SaleType saleType, Long sourceId, BigDecimal amount, LocalDateTime occurredAt) {
        String sourceType = saleType.name();
        return salesRepository.findBySourceTypeAndSourceId(sourceType, sourceId)
                .orElseGet(() -> saveNewSales(payment, saleType, sourceId, amount, occurredAt));
    }

    @Transactional(readOnly = true)
    public SalesLedgerPageResponse getSalesLedger(
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String ledgerStatus,
            String settlementStatus,
            String keyword,
            int page,
            int size) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        int normalizedSize = size <= 0 ? 15 : Math.min(size, 100);
        int normalizedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        Page<SalesTransaction> result = salesRepository.searchLedger(
                start,
                end,
                parseSaleType(transactionType),
                parseLedgerStatus(ledgerStatus),
                parseSettlementStatus(settlementStatus),
                normalizeKeyword(keyword),
                pageable
        );
        return new SalesLedgerPageResponse(
                result.getContent().stream().map(SalesResponse::from).toList(),
                result.getTotalElements(),
                normalizedPage,
                normalizedSize,
                summarizeSalesLedger(start, end, transactionType, ledgerStatus, settlementStatus, keyword)
        );
    }

    @Transactional(readOnly = true)
    public SalesLedgerSummaryResponse getSalesLedgerSummary(
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String ledgerStatus,
            String settlementStatus,
            String keyword) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return summarizeSalesLedger(start, end, transactionType, ledgerStatus, settlementStatus, keyword);
    }

    @Transactional(readOnly = true)
    public List<SalesResponse> getSales(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return salesRepository.findByBusinessDateBetweenOrderByOccurredAtDesc(start, end).stream()
                .map(SalesResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesResponse getSalesLedgerDetail(Long salesTransactionId) {
        return salesRepository.findById(salesTransactionId)
                .map(SalesResponse::from)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALES_TRANSACTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public SalesLedgerLinksResponse getSalesLedgerLinks(Long salesTransactionId) {
        SalesTransaction sales = salesRepository.findById(salesTransactionId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALES_TRANSACTION_NOT_FOUND));
        SalesResponse originalSale = sales.getOriginalSalesTransactionId() == null ? null
                : salesRepository.findById(sales.getOriginalSalesTransactionId()).map(SalesResponse::from).orElse(null);
        PaymentTransaction payment = sales.getPaymentId() == null ? null
                : paymentRepository.findById(sales.getPaymentId()).orElse(null);
        PaymentCancel cancel = sales.getCancelId() == null ? null
                : cancelRepository.findById(sales.getCancelId()).orElse(null);
        List<RecoveryTaskResponse> recoveryTasks = sales.getPaymentId() == null
                ? List.of()
                : recoveryTaskRepository.findByPaymentIdOrderByIdAsc(sales.getPaymentId()).stream()
                .map(RecoveryTaskResponse::from)
                .toList();

        return new SalesLedgerLinksResponse(
                sales.getId(),
                originalSale,
                payment == null ? null : PaymentResponse.from(payment),
                cancel == null ? null : cancel.getId(),
                payment == null ? BigDecimal.ZERO : payment.getCanceledAmount(),
                payment == null ? BigDecimal.ZERO : payment.getCancelableAmount(),
                cancel == null ? null : cancel.getCancelReason(),
                cancel == null ? null : cancel.getCanceledAt(),
                externalSendRequestRepository.findBySalesIdOrderByIdAsc(sales.getId()).stream()
                        .map(ExternalSendResponse::from)
                        .toList(),
                alimtalkQueueRepository.findBySalesIdOrderByIdAsc(sales.getId()).stream()
                        .map(AlimtalkQueueResponse::from)
                        .toList(),
                recoveryTasks,
                settlementDetailRepository.findBySalesIdOrderByIdAsc(sales.getId()).stream()
                        .map(SettlementDetailResponse::from)
                        .toList()
        );
    }

    @Transactional
    public void addSalesAdjustment(Long salesId, SalesAdjustmentRequest request) {
        if (!StringUtils.hasText(request.adjustmentType()) || request.adjustmentAmount() == null) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "조정 유형과 조정금액은 필수입니다.");
        }
        salesRepository.findById(salesId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALES_TRANSACTION_NOT_FOUND));
        adjustmentRepository.save(SettlementAdjustment.builder()
                .salesId(salesId)
                .adjustmentType(request.adjustmentType())
                .adjustmentAmount(request.adjustmentAmount())
                .reason(StringUtils.hasText(request.reason()) ? request.reason() : "Portfolio mock adjustment")
                .build());
        saveAudit("SALES", "ADJUSTMENT", String.valueOf(salesId), "매출 정산 조정 데이터를 등록했습니다.");
    }

    private SalesTransaction saveNewSales(PaymentTransaction payment, SaleType saleType, Long sourceId, BigDecimal amount, LocalDateTime occurredAt) {
        BigDecimal totalAmount = amount.setScale(0, RoundingMode.HALF_UP);
        BigDecimal supplyAmount = calculateSupplyAmount(totalAmount);
        BigDecimal vatAmount = totalAmount.subtract(supplyAmount);
        Long cancelId = SaleType.CANCEL.equals(saleType) ? sourceId : null;
        Long originalSalesTransactionId = SaleType.CANCEL.equals(saleType) ? findOriginalSaleId(payment.getOrderNo()) : null;
        SalesTransaction sales = SalesTransaction.builder()
                .sourceType(saleType.name())
                .sourceId(sourceId)
                .paymentId(payment.getId())
                .cancelId(cancelId)
                .originalSalesTransactionId(originalSalesTransactionId)
                .orderNo(payment.getOrderNo())
                .tid(payment.getTid())
                .pgTransactionId(payment.getTid())
                .saleType(saleType)
                .saleAmount(totalAmount)
                .supplyAmount(supplyAmount)
                .vatAmount(vatAmount)
                .totalAmount(totalAmount)
                .saleStatus(SaleStatus.READY)
                .ledgerStatus(LedgerStatus.POSTED)
                .settlementStatus(SalesSettlementStatus.NOT_SETTLED)
                .businessDate(occurredAt.toLocalDate())
                .occurredAt(occurredAt)
                .pgCode(payment.getPgProvider() == null ? "INICIS" : payment.getPgProvider().name())
                .paymentMethod(PaymentDefaults.PAYMENT_METHOD_CARD)
                .externalSendRequired(true)
                .settlementIncludedYn(false)
                .build();
        try {
            return salesRepository.save(sales);
        } catch (DataIntegrityViolationException duplicate) {
            return salesRepository.findBySourceTypeAndSourceId(saleType.name(), sourceId)
                    .orElseThrow(() -> duplicate);
        }
    }

    private SalesLedgerSummaryResponse summarizeSalesLedger(
            LocalDate start,
            LocalDate end,
            String transactionType,
            String ledgerStatus,
            String settlementStatus,
            String keyword) {
        return salesRepository.summarizeLedger(
                start,
                end,
                parseSaleType(transactionType),
                parseLedgerStatus(ledgerStatus),
                parseSettlementStatus(settlementStatus),
                normalizeKeyword(keyword)
        );
    }

    private BigDecimal calculateSupplyAmount(BigDecimal totalAmount) {
        return totalAmount.multiply(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(11), 0, RoundingMode.HALF_UP);
    }

    private Long findOriginalSaleId(String orderNo) {
        return salesRepository.findFirstByOrderNoAndSaleTypeOrderByIdAsc(orderNo, SaleType.SALE)
                .map(SalesTransaction::getId)
                .orElse(null);
    }

    private SaleType parseSaleType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return SaleType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationBusinessException(ErrorCode.SALES_INVALID_FILTER, "지원하지 않는 거래유형입니다. 허용 값: SALE, CANCEL, ADJUST");
        }
    }

    private LedgerStatus parseLedgerStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LedgerStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationBusinessException(ErrorCode.SALES_INVALID_FILTER, "지원하지 않는 원장상태입니다.");
        }
    }

    private SalesSettlementStatus parseSettlementStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return SalesSettlementStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationBusinessException(ErrorCode.SALES_INVALID_FILTER, "지원하지 않는 정산상태입니다.");
        }
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private void saveAudit(String domainType, String actionType, String referenceKey, String description) {
        auditLogRepository.save(AuditLog.builder()
                .domainType(domainType)
                .actionType(actionType)
                .referenceKey(referenceKey)
                .description(description)
                .loggedAt(LocalDateTime.now())
                .build());
    }
}
