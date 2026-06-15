package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExternalSendRequestRepository extends JpaRepository<ExternalSendRequest, Long> {

    Optional<ExternalSendRequest> findByRequestKey(String requestKey);

    List<ExternalSendRequest> findBySendStatusOrderByIdAsc(ExternalSendStatus sendStatus);

    List<ExternalSendRequest> findBySalesIdOrderByIdAsc(Long salesId);

    @Query("""
            select r.id from ExternalSendRequest r
             where r.sendStatus in :statuses
               and r.retryCount < r.maxRetryCount
             order by r.id asc
            """)
    List<Long> findProcessableIds(@Param("statuses") Collection<ExternalSendStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ExternalSendRequest r
               set r.sendStatus = :sendingStatus,
                   r.processingStartedAt = :now
             where r.id = :id
               and r.sendStatus in :claimableStatuses
               and r.retryCount < r.maxRetryCount
            """)
    int claimForSend(
            @Param("id") Long id,
            @Param("sendingStatus") ExternalSendStatus sendingStatus,
            @Param("claimableStatuses") Collection<ExternalSendStatus> claimableStatuses,
            @Param("now") LocalDateTime now
    );
}
