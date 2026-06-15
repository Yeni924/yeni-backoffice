package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlimtalkQueueRepository extends JpaRepository<AlimtalkQueue, Long> {

    Optional<AlimtalkQueue> findByMessageKey(String messageKey);

    List<AlimtalkQueue> findByStatusOrderByIdAsc(AlimtalkStatus status);

    List<AlimtalkQueue> findBySalesIdOrderByIdAsc(Long salesId);

    @Query("""
            select q.id from AlimtalkQueue q
             where q.status in :statuses
               and q.retryCount < q.maxRetryCount
             order by q.id asc
            """)
    List<Long> findProcessableIds(@Param("statuses") Collection<AlimtalkStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AlimtalkQueue q
               set q.status = :sendingStatus,
                   q.processingStartedAt = :now
             where q.id = :id
               and q.status in :claimableStatuses
               and q.retryCount < q.maxRetryCount
            """)
    int claimForSend(
            @Param("id") Long id,
            @Param("sendingStatus") AlimtalkStatus sendingStatus,
            @Param("claimableStatuses") Collection<AlimtalkStatus> claimableStatuses,
            @Param("now") LocalDateTime now
    );
}
