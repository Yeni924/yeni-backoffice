package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.BusinessException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentQueryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.entity.AuditLog;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.repository.AuditLogRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentRecoveryOperationService {

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final PaymentQueryService paymentQueryService;
    private final AuditLogRepository auditLogRepository;

    public PaymentRecoveryOperationService(
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            PaymentQueryService paymentQueryService,
            AuditLogRepository auditLogRepository) {
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.paymentQueryService = paymentQueryService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public RecoveryTaskPageResponse getTasks(
            String status,
            String recoveryType,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {
        RecoveryStatus parsedStatus = parseStatus(status);
        RecoveryType parsedType = parseType(recoveryType);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        LocalDate start = startDate == null ? LocalDate.now().minusDays(30) : startDate;
        LocalDate end = endDate == null ? LocalDate.now().plusDays(1) : endDate.plusDays(1);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<RecoveryTaskResponse> filtered = recoveryTaskRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(task -> parsedStatus == null || parsedStatus.equals(task.getStatus()))
                .filter(task -> parsedType == null || parsedType.equals(task.getRecoveryType()))
                .filter(task -> matchesKeyword(task, normalizedKeyword))
                .filter(task -> !task.getCreatedAt().toLocalDate().isBefore(start) && task.getCreatedAt().toLocalDate().isBefore(end))
                .map(RecoveryTaskResponse::from)
                .toList();
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        return new RecoveryTaskPageResponse(filtered.subList(fromIndex, toIndex), filtered.size(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public RecoveryTaskResponse getTask(Long taskId) {
        return RecoveryTaskResponse.from(findTask(taskId));
    }

    @Transactional
    public RecoveryTaskResponse retry(Long taskId) {
        PaymentRecoveryTask task = findTask(taskId);
        validateRetryable(task);
        task.markProcessing();
        saveAudit("RECOVERY_TASK", "RETRY_REQUESTED", task.getTaskKey(), "운영자가 복구 작업 재시도를 요청했습니다.");

        if (RecoveryType.APPROVE_UNKNOWN_CHECK.equals(task.getRecoveryType())) {
            if (task.getPaymentId() == null) {
                task.markFailed("paymentId가 없어 자동 승인 재조회가 불가능합니다. orderNo/tid/idempotencyKey 기준으로 수동 확인이 필요합니다.");
                return RecoveryTaskResponse.from(task);
            }
            PaymentQueryResponse result = paymentQueryService.retryQuery(task.getPaymentId());
            if (PaymentStatus.APPROVED.name().equals(result.paymentStatus())) {
                task.markSuccess();
                saveAudit("RECOVERY_TASK", "RETRY_SUCCESS", task.getTaskKey(), "승인 결과불명 재조회가 성공했습니다.");
            } else if (PaymentStatus.APPROVE_FAILED.name().equals(result.paymentStatus())) {
                task.markFailed(result.resultMessage());
                saveAudit("RECOVERY_TASK", "RETRY_FAILED", task.getTaskKey(), "승인 결과불명 재조회가 실패로 확정되었습니다.");
            } else {
                task.markReady(result.resultMessage());
                saveAudit("RECOVERY_TASK", "RETRY_PENDING", task.getTaskKey(), "승인 결과불명 재조회 결과가 아직 확정되지 않았습니다.");
            }
            return RecoveryTaskResponse.from(task);
        }

        if (RecoveryType.CANCEL_UNKNOWN_CHECK.equals(task.getRecoveryType())) {
            task.markReady("현재 Mock PG Gateway에는 취소 결과 전용 조회 API가 없어 자동 재시도를 지원하지 않습니다.");
            throw new BusinessException(ErrorCode.RECOVERY_RETRY_NOT_ALLOWED);
        }

        task.markReady("현재 복구 유형은 자동 재시도를 지원하지 않습니다.");
        throw new BusinessException(ErrorCode.RECOVERY_RETRY_NOT_ALLOWED);
    }

    @Transactional
    public RecoveryTaskResponse markSuccess(Long taskId) {
        PaymentRecoveryTask task = findTask(taskId);
        if (RecoveryStatus.SUCCESS.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.RECOVERY_TASK_ALREADY_COMPLETED);
        }
        task.markSuccess();
        saveAudit("RECOVERY_TASK", "MARK_SUCCESS", task.getTaskKey(), "운영자가 복구 작업을 성공 처리했습니다.");
        return RecoveryTaskResponse.from(task);
    }

    @Transactional
    public RecoveryTaskResponse markFailed(Long taskId) {
        PaymentRecoveryTask task = findTask(taskId);
        if (RecoveryStatus.SUCCESS.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.RECOVERY_TASK_ALREADY_COMPLETED);
        }
        task.markFailed("운영자가 복구 작업을 실패 처리했습니다.");
        saveAudit("RECOVERY_TASK", "MARK_FAILED", task.getTaskKey(), "운영자가 복구 작업을 실패 처리했습니다.");
        return RecoveryTaskResponse.from(task);
    }

    private PaymentRecoveryTask findTask(Long taskId) {
        return recoveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RECOVERY_TASK_NOT_FOUND));
    }

    private void validateRetryable(PaymentRecoveryTask task) {
        if (RecoveryStatus.SUCCESS.equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.RECOVERY_TASK_ALREADY_COMPLETED);
        }
        if (task.getRetryCount() >= task.getMaxRetryCount()) {
            throw new BusinessException(ErrorCode.RECOVERY_RETRY_NOT_ALLOWED, "복구 작업 최대 재시도 횟수를 초과했습니다.");
        }
    }

    private RecoveryStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return RecoveryStatus.valueOf(status.trim().toUpperCase());
    }

    private RecoveryType parseType(String recoveryType) {
        if (!StringUtils.hasText(recoveryType)) {
            return null;
        }
        return RecoveryType.valueOf(recoveryType.trim().toUpperCase());
    }

    private boolean matchesKeyword(PaymentRecoveryTask task, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(task.getTaskKey(), keyword)
                || contains(task.getOrderNo(), keyword)
                || contains(task.getTid(), keyword)
                || contains(task.getIdempotencyKey(), keyword)
                || contains(task.getLastErrorMessage(), keyword)
                || contains(task.getPaymentId(), keyword)
                || contains(task.getCancelId(), keyword);
    }

    private boolean contains(Object value, String keyword) {
        return value != null && String.valueOf(value).toLowerCase().contains(keyword);
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
