package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecoveryTaskClaimService {

    private static final List<RecoveryStatus> CLAIMABLE_STATUSES =
            List.of(RecoveryStatus.READY, RecoveryStatus.FAILED);

    private final PaymentRecoveryTaskRepository recoveryTaskRepository;

    public RecoveryTaskClaimService(PaymentRecoveryTaskRepository recoveryTaskRepository) {
        this.recoveryTaskRepository = recoveryTaskRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long taskId) {
        return recoveryTaskRepository.claimForRetry(
                taskId,
                RecoveryStatus.PROCESSING,
                CLAIMABLE_STATUSES,
                LocalDateTime.now()
        ) == 1;
    }
}
