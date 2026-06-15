package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpWorkerFailureService {

    private static final int EXTERNAL_ERROR_MAX_LENGTH = 200;
    private static final int ALIMTALK_ERROR_MAX_LENGTH = 300;

    private final ExternalSendRequestRepository externalRepository;
    private final AlimtalkQueueRepository alimtalkRepository;

    public FollowUpWorkerFailureService(
            ExternalSendRequestRepository externalRepository,
            AlimtalkQueueRepository alimtalkRepository) {
        this.externalRepository = externalRepository;
        this.alimtalkRepository = alimtalkRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failExternalSend(Long id, String message) {
        externalRepository.findById(id)
                .filter(request -> ExternalSendStatus.SENDING.equals(request.getSendStatus()))
                .ifPresent(request -> request.markFailed(normalize(message, EXTERNAL_ERROR_MAX_LENGTH)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAlimtalk(Long id, String message) {
        alimtalkRepository.findById(id)
                .filter(queue -> AlimtalkStatus.SENDING.equals(queue.getStatus()))
                .ifPresent(queue -> queue.markFailed(normalize(message, ALIMTALK_ERROR_MAX_LENGTH)));
    }

    private String normalize(String message, int maxLength) {
        String normalized = message == null || message.isBlank()
                ? "Mock Worker 처리 중 예상하지 못한 오류가 발생했습니다."
                : message;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
