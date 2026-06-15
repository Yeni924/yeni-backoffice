package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.client.ExternalSendClient;
import com.yeni.backoffice.core.payment.client.ExternalSendResult;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerItemResult;
import com.yeni.backoffice.core.payment.entity.ExternalSendHistory;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.repository.ExternalSendHistoryRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExternalSendWorkerProcessor {

    private final ExternalSendRequestRepository requestRepository;
    private final ExternalSendHistoryRepository historyRepository;
    private final ExternalSendClient client;

    public ExternalSendWorkerProcessor(
            ExternalSendRequestRepository requestRepository,
            ExternalSendHistoryRepository historyRepository,
            ExternalSendClient client) {
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.client = client;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkerItemResult process(Long id) {
        ExternalSendRequest request = requestRepository.findById(id).orElseThrow();
        String requestBody = "salesId=" + request.getSalesId() + ", requestKey=" + request.getRequestKey();
        try {
            ExternalSendResult result = client.send(request);
            if (result.success()) {
                request.markSuccess();
            } else {
                request.markFailed(result.message());
            }
            saveHistory(request, requestBody, result.externalResponseCode(),
                    result.success() ? "SUCCESS" : "FAILED", result.message());
            return new WorkerItemResult(result.success());
        } catch (Exception exception) {
            request.markFailed(exception.getMessage());
            saveHistory(request, requestBody, null, "FAILED", exception.getMessage());
            return new WorkerItemResult(false);
        }
    }

    private void saveHistory(
            ExternalSendRequest request,
            String requestBody,
            String responseBody,
            String resultStatus,
            String resultMessage) {
        historyRepository.save(ExternalSendHistory.builder()
                .sendRequestId(request.getId())
                .targetSystem(request.getTargetSystem())
                .requestBody(requestBody)
                .responseBody(responseBody)
                .resultStatus(resultStatus)
                .resultMessage(resultMessage)
                .sentAt(LocalDateTime.now())
                .build());
    }
}
