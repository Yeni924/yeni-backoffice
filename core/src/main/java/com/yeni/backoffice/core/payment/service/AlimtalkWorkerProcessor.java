package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.client.AlimtalkClient;
import com.yeni.backoffice.core.payment.client.AlimtalkSendResult;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerItemResult;
import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlimtalkWorkerProcessor {

    private final AlimtalkQueueRepository repository;
    private final AlimtalkClient client;

    public AlimtalkWorkerProcessor(AlimtalkQueueRepository repository, AlimtalkClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkerItemResult process(Long id) {
        AlimtalkQueue queue = repository.findById(id).orElseThrow();
        try {
            AlimtalkSendResult result = client.send(queue);
            if (result.success()) {
                queue.markSuccess();
            } else {
                queue.markFailed(result.message());
            }
            return new WorkerItemResult(result.success());
        } catch (Exception exception) {
            queue.markFailed(exception.getMessage());
            return new WorkerItemResult(false);
        }
    }
}
