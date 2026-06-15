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

    private static final int AUDIT_REFERENCE_KEY_MAX_LENGTH = 80;

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final PaymentQueryService paymentQueryService;
    private final AuditLogRepository auditLogRepository;
    private final RecoveryTaskClaimService recoveryTaskClaimService;
    private final RecoveryTaskStateService recoveryTaskStateService;

    public PaymentRecoveryOperationService(
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            PaymentQueryService paymentQueryService,
            AuditLogRepository auditLogRepository,
            RecoveryTaskClaimService recoveryTaskClaimService,
            RecoveryTaskStateService recoveryTaskStateService) {
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.paymentQueryService = paymentQueryService;
        this.auditLogRepository = auditLogRepository;
        this.recoveryTaskClaimService = recoveryTaskClaimService;
        this.recoveryTaskStateService = recoveryTaskStateService;
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

    public RecoveryTaskResponse retry(Long taskId) {
        if (!recoveryTaskClaimService.claim(taskId)) {
            throw new BusinessException(ErrorCode.RECOVERY_TASK_NOT_CLAIMABLE);
        }

        PaymentRecoveryTask task = findTask(taskId);

        try {
            saveAudit("RECOVERY_TASK", "RETRY_REQUESTED", task.getTaskKey(), "운영자가 복구 작업 재시도를 요청했습니다.");
            if (RecoveryType.APPROVE_UNKNOWN_CHECK.equals(task.getRecoveryType())) {
                return retryApproveUnknown(task);
            }
            throw new BusinessException(
                    ErrorCode.RECOVERY_RETRY_NOT_ALLOWED,
                    "현재 포트폴리오에서는 해당 복구 유형의 자동 재처리를 지원하지 않습니다."
            );
        } catch (Exception exception) {
            recoveryTaskStateService.markFailed(taskId, exception.getMessage());
            saveAudit("RECOVERY_TASK", "RETRY_FAILED", task.getTaskKey(), "복구 작업 재처리가 실패했습니다.");
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(exception);
        }
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

    private RecoveryTaskResponse retryApproveUnknown(PaymentRecoveryTask task) {
        if (task.getPaymentId() == null) {
            return recoveryTaskStateService.markFailed(
                    task.getId(),
                    "paymentId가 없어 자동 승인 재조회가 불가능합니다. orderNo/tid/idempotencyKey 기준으로 수동 확인이 필요합니다."
            );
        }

        PaymentQueryResponse result = paymentQueryService.retryQuery(task.getPaymentId());
        if (PaymentStatus.APPROVED.name().equals(result.paymentStatus())) {
            saveAudit("RECOVERY_TASK", "RETRY_SUCCESS", task.getTaskKey(), "승인 결과불명 재조회가 성공했습니다.");
            return recoveryTaskStateService.markSuccess(task.getId());
        }
        if (PaymentStatus.APPROVE_FAILED.name().equals(result.paymentStatus())) {
            saveAudit("RECOVERY_TASK", "RETRY_FAILED", task.getTaskKey(), "승인 결과불명 재조회가 실패로 확정되었습니다.");
            return recoveryTaskStateService.markFailed(task.getId(), result.resultMessage());
        }
        saveAudit("RECOVERY_TASK", "RETRY_PENDING", task.getTaskKey(), "승인 결과불명 재조회 결과가 아직 확정되지 않았습니다.");
        return recoveryTaskStateService.markReady(task.getId(), result.resultMessage());
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
                .referenceKey(truncate(referenceKey, AUDIT_REFERENCE_KEY_MAX_LENGTH))
                .description(description)
                .loggedAt(LocalDateTime.now())
                .build());
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
