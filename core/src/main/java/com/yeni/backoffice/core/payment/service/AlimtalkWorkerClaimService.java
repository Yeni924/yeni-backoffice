package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlimtalkWorkerClaimService {

    private static final List<AlimtalkStatus> CLAIMABLE_STATUSES =
            List.of(AlimtalkStatus.READY, AlimtalkStatus.FAILED, AlimtalkStatus.RETRY_READY);

    private final AlimtalkQueueRepository repository;

    public AlimtalkWorkerClaimService(AlimtalkQueueRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long id) {
        return repository.claimForSend(id, AlimtalkStatus.SENDING, CLAIMABLE_STATUSES, LocalDateTime.now()) == 1;
    }
}
