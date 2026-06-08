package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlimtalkQueueRepository extends JpaRepository<AlimtalkQueue, Long> {

    Optional<AlimtalkQueue> findByMessageKey(String messageKey);

    List<AlimtalkQueue> findByStatusOrderByIdAsc(AlimtalkStatus status);

    List<AlimtalkQueue> findBySalesIdOrderByIdAsc(Long salesId);
}
