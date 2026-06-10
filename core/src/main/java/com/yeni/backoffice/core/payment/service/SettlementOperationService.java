package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.ConflictException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.payment.config.InicisStdPayProperties;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementBatchRunRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementFeeDetailResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementStatementResponse;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementLog;
import com.yeni.backoffice.core.payment.entity.SettlementStatement;
import com.yeni.backoffice.core.payment.enums.SettlementStatus;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementDetailRepository;
import com.yeni.backoffice.core.payment.repository.SettlementFeeDetailRepository;
import com.yeni.backoffice.core.payment.repository.SettlementLogRepository;
import com.yeni.backoffice.core.payment.repository.SettlementStatementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SettlementOperationService {

    private final InicisStdPayProperties inicisProperties;
    private final SalesTransactionRepository salesRepository;
    private final SettlementStatementRepository settlementStatementRepository;
    private final SettlementDetailRepository settlementDetailRepository;
    private final SettlementFeeDetailRepository settlementFeeDetailRepository;
    private final SettlementLogRepository settlementLogRepository;
    private final SettlementBatchProcessor settlementBatchProcessor;
    private final ConcurrentMap<String, ReentrantLock> executionLocks = new ConcurrentHashMap<>();

    public SettlementOperationService(
            InicisStdPayProperties inicisProperties,
            SalesTransactionRepository salesRepository,
            SettlementStatementRepository settlementStatementRepository,
            SettlementDetailRepository settlementDetailRepository,
            SettlementFeeDetailRepository settlementFeeDetailRepository,
            SettlementLogRepository settlementLogRepository,
            SettlementBatchProcessor settlementBatchProcessor) {
        this.inicisProperties = inicisProperties;
        this.salesRepository = salesRepository;
        this.settlementStatementRepository = settlementStatementRepository;
        this.settlementDetailRepository = settlementDetailRepository;
        this.settlementFeeDetailRepository = settlementFeeDetailRepository;
        this.settlementLogRepository = settlementLogRepository;
        this.settlementBatchProcessor = settlementBatchProcessor;
    }

    public SettlementStatementResponse runDailySettlement(SettlementBatchRunRequest request) {
        LocalDate targetDate = request == null || request.targetDate() == null
                ? LocalDate.now().minusDays(1)
                : request.targetDate();
        String mid = inicisProperties.getMid();
        String lockKey = targetDate + ":" + mid;
        ReentrantLock executionLock = executionLocks.computeIfAbsent(lockKey, ignored -> new ReentrantLock());

        if (!executionLock.tryLock()) {
            throw duplicateExecution(targetDate, mid);
        }

        try {
            if (settlementStatementRepository.findBySettlementDateAndMid(targetDate, mid).isPresent()) {
                throw duplicateExecution(targetDate, mid);
            }
            return settlementBatchProcessor.process(targetDate, mid);
        } catch (DataIntegrityViolationException duplicate) {
            throw duplicateExecution(targetDate, mid);
        } finally {
            executionLock.unlock();
            executionLocks.remove(lockKey, executionLock);
        }
    }

    @Transactional(readOnly = true)
    public List<SettlementStatementResponse> getStatements(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return settlementStatementRepository.findBySettlementDateBetweenOrderBySettlementDateDesc(start, end).stream()
                .map(SettlementStatementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementDetailPageResponse getStatement(Long statementId) {
        SettlementStatement statement = settlementStatementRepository.findById(statementId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SETTLEMENT_NOT_FOUND));
        List<SettlementDetailResponse> details = settlementDetailRepository.findBySettlementStatementIdOrderByIdAsc(statementId).stream()
                .map(SettlementDetailResponse::from)
                .toList();
        List<SettlementFeeDetailResponse> feeDetails = settlementFeeDetailRepository.findBySettlementStatementIdOrderByIdAsc(statementId).stream()
                .map(SettlementFeeDetailResponse::from)
                .toList();
        return new SettlementDetailPageResponse(SettlementStatementResponse.from(statement), details, feeDetails);
    }

    @Transactional
    public SettlementStatementResponse confirmStatement(Long statementId) {
        SettlementStatement statement = settlementStatementRepository.findById(statementId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (!SettlementStatus.DRAFT.equals(statement.getSettlementStatus())) {
            throw new ConflictException(ErrorCode.SETTLEMENT_INVALID_STATUS, "작성 중인 정산명세만 확정할 수 있습니다.");
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (!SettlementStatus.CONFIRMED.equals(statement.getSettlementStatus())) {
            throw new ConflictException(ErrorCode.SETTLEMENT_INVALID_STATUS, "확정된 정산명세만 지급 처리할 수 있습니다.");
        }
        settlementDetailRepository.findBySettlementStatementIdOrderByIdAsc(statementId)
                .forEach(detail -> salesRepository.findById(detail.getSalesId()).ifPresent(SalesTransaction::markPaid));
        statement.markPaid();
        saveSettlementLog(statementId, "PAY", "SUCCESS", "정산 지급 처리 완료");
        return SettlementStatementResponse.from(statement);
    }

    private ConflictException duplicateExecution(LocalDate targetDate, String mid) {
        return new ConflictException(
                ErrorCode.SETTLEMENT_DUPLICATE_EXECUTION,
                "동일 정산일과 MID 기준의 정산 배치가 이미 실행 중이거나 완료되었습니다. targetDate="
                        + targetDate + ", mid=" + mid
        );
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
