package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.ExternalSendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalSendHistoryRepository extends JpaRepository<ExternalSendHistory, Long> {

    List<ExternalSendHistory> findBySendRequestIdOrderBySentAtDesc(Long sendRequestId);
}
