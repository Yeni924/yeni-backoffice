package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerItemResult;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerRunResult;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalSendWorkerService {

    private static final List<ExternalSendStatus> PROCESSABLE_STATUSES =
            List.of(ExternalSendStatus.READY, ExternalSendStatus.FAILED);

    private final ExternalSendRequestRepository repository;
    private final ExternalSendWorkerClaimService claimService;
    private final ExternalSendWorkerProcessor processor;
    private final FollowUpWorkerFailureService failureService;

    public ExternalSendWorkerService(
            ExternalSendRequestRepository repository,
            ExternalSendWorkerClaimService claimService,
            ExternalSendWorkerProcessor processor,
            FollowUpWorkerFailureService failureService) {
        this.repository = repository;
        this.claimService = claimService;
        this.processor = processor;
        this.failureService = failureService;
    }

    public WorkerRunResult processReadyRequests(int limit) {
        List<Long> targetIds = repository.findProcessableIds(PROCESSABLE_STATUSES).stream()
                .limit(Math.min(Math.max(limit, 1), 100))
                .toList();
        int claimed = 0;
        int success = 0;
        int failure = 0;

        for (Long id : targetIds) {
            if (!claimService.claim(id)) {
                continue;
            }
            claimed++;
            try {
                WorkerItemResult result = processor.process(id);
                if (result.success()) {
                    success++;
                } else {
                    failure++;
                }
            } catch (Exception exception) {
                failureService.failExternalSend(id, exception.getMessage());
                failure++;
            }
        }
        return new WorkerRunResult(targetIds.size(), claimed, success, failure, targetIds.size() - claimed);
    }
}
