package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryTaskStateService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 300;

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;

    public RecoveryTaskStateService(PaymentRecoveryTaskRepository recoveryTaskRepository) {
        this.recoveryTaskRepository = recoveryTaskRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecoveryTaskResponse markSuccess(Long taskId) {
        PaymentRecoveryTask task = findTask(taskId);
        if (!RecoveryStatus.SUCCESS.equals(task.getStatus())) {
            task.markSuccess();
        }
        return RecoveryTaskResponse.from(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecoveryTaskResponse markFailed(Long taskId, String errorMessage) {
        PaymentRecoveryTask task = findTask(taskId);
        if (RecoveryStatus.PROCESSING.equals(task.getStatus())) {
            task.markFailed(normalizeErrorMessage(errorMessage));
        }
        return RecoveryTaskResponse.from(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecoveryTaskResponse markReady(Long taskId, String errorMessage) {
        PaymentRecoveryTask task = findTask(taskId);
        if (RecoveryStatus.PROCESSING.equals(task.getStatus())) {
            task.markReady(normalizeErrorMessage(errorMessage));
        }
        return RecoveryTaskResponse.from(task);
    }

    private PaymentRecoveryTask findTask(Long taskId) {
        return recoveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RECOVERY_TASK_NOT_FOUND));
    }

    private String normalizeErrorMessage(String errorMessage) {
        String message = errorMessage == null || errorMessage.isBlank()
                ? "복구 작업 재처리 중 예상하지 못한 오류가 발생했습니다."
                : errorMessage;
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
