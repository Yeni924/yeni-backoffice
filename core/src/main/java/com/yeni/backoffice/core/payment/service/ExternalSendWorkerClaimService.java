package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExternalSendWorkerClaimService {

    private static final List<ExternalSendStatus> CLAIMABLE_STATUSES =
            List.of(ExternalSendStatus.READY, ExternalSendStatus.FAILED);

    private final ExternalSendRequestRepository repository;

    public ExternalSendWorkerClaimService(ExternalSendRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long id) {
        return repository.claimForSend(id, ExternalSendStatus.SENDING, CLAIMABLE_STATUSES, LocalDateTime.now()) == 1;
    }
}
