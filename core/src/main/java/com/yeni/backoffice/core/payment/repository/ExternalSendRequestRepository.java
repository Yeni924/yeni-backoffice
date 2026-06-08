package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalSendRequestRepository extends JpaRepository<ExternalSendRequest, Long> {

    Optional<ExternalSendRequest> findByRequestKey(String requestKey);

    List<ExternalSendRequest> findBySendStatusOrderByIdAsc(ExternalSendStatus sendStatus);

    List<ExternalSendRequest> findBySalesIdOrderByIdAsc(Long salesId);
}
