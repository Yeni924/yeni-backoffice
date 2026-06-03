package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.BatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settlement_batch_log")
public class SettlementBatchLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus batchStatus;

    @Column(nullable = false)
    private Integer targetCount;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false)
    private Integer failureCount;

    @Column(length = 200)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    public void complete(int successCount, int failureCount) {
        this.batchStatus = BatchStatus.SUCCESS;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.endedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.batchStatus = BatchStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endedAt = LocalDateTime.now();
    }
}
