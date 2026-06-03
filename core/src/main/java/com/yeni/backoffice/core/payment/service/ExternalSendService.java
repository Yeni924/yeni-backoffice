package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.ExternalSendResponse;
import com.yeni.backoffice.core.payment.entity.ExternalSendHistory;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.ExternalSendHistoryRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExternalSendService {

    private final ExternalSendRequestRepository sendRequestRepository;
    private final ExternalSendHistoryRepository sendHistoryRepository;

    public ExternalSendService(
            ExternalSendRequestRepository sendRequestRepository,
            ExternalSendHistoryRepository sendHistoryRepository) {
        this.sendRequestRepository = sendRequestRepository;
        this.sendHistoryRepository = sendHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ExternalSendResponse> getRequests(String status) {
        List<ExternalSendRequest> requests = status == null
                ? sendRequestRepository.findAll()
                : sendRequestRepository.findBySendStatusOrderByIdAsc(ExternalSendStatus.valueOf(status));
        return requests.stream()
                .map(ExternalSendResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExternalSendResponse send(Long requestId) {
        ExternalSendRequest request = sendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("External send request not found."));

        String requestBody = "salesId=" + request.getSalesId() + ", requestKey=" + request.getRequestKey();
        if (request.getRequestKey().toUpperCase().contains("FAIL")) {
            request.markFailed("Portfolio mock external send failure.");
            saveHistory(request, requestBody, "FAIL", "FAILED", request.getLastErrorMessage());
            return ExternalSendResponse.from(request);
        }

        request.markSuccess();
        saveHistory(request, requestBody, "OK", "SUCCESS", "Mock external send success.");
        return ExternalSendResponse.from(request);
    }

    @Transactional
    public List<ExternalSendResponse> retryFailed() {
        return sendRequestRepository.findBySendStatusOrderByIdAsc(ExternalSendStatus.FAILED).stream()
                .map(request -> send(request.getId()))
                .collect(Collectors.toList());
    }

    private void saveHistory(
            ExternalSendRequest request,
            String requestBody,
            String responseBody,
            String resultStatus,
            String resultMessage) {
        sendHistoryRepository.save(ExternalSendHistory.builder()
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
