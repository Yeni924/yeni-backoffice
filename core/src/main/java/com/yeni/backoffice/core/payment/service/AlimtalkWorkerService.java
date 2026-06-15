package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerItemResult;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerRunResult;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlimtalkWorkerService {

    private static final List<AlimtalkStatus> PROCESSABLE_STATUSES =
            List.of(AlimtalkStatus.READY, AlimtalkStatus.FAILED, AlimtalkStatus.RETRY_READY);

    private final AlimtalkQueueRepository repository;
    private final AlimtalkWorkerClaimService claimService;
    private final AlimtalkWorkerProcessor processor;
    private final FollowUpWorkerFailureService failureService;

    public AlimtalkWorkerService(
            AlimtalkQueueRepository repository,
            AlimtalkWorkerClaimService claimService,
            AlimtalkWorkerProcessor processor,
            FollowUpWorkerFailureService failureService) {
        this.repository = repository;
        this.claimService = claimService;
        this.processor = processor;
        this.failureService = failureService;
    }

    public WorkerRunResult processReadyMessages(int limit) {
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
                failureService.failAlimtalk(id, exception.getMessage());
                failure++;
            }
        }
        return new WorkerRunResult(targetIds.size(), claimed, success, failure, targetIds.size() - claimed);
    }
}
