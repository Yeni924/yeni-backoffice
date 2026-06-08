package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "payment_recovery_task",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_recovery_task_key", columnNames = "taskKey")
)
public class PaymentRecoveryTask extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String taskKey;

    private Long paymentId;

    private Long cancelId;

    @Column(length = 80)
    private String orderNo;

    @Column(length = 120)
    private String tid;

    @Column(length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryType recoveryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecoveryStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Integer maxRetryCount;

    @Column(length = 300)
    private String lastErrorMessage;

    private LocalDateTime lastTriedAt;

    private LocalDateTime processedAt;

    public void markProcessing() {
        this.status = RecoveryStatus.PROCESSING;
        this.lastTriedAt = LocalDateTime.now();
    }

    public void markSuccess() {
        this.status = RecoveryStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
        this.lastTriedAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = RecoveryStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.processedAt = LocalDateTime.now();
        this.lastTriedAt = LocalDateTime.now();
        this.lastErrorMessage = errorMessage;
    }

    public void markReady(String errorMessage) {
        this.status = RecoveryStatus.READY;
        this.retryCount = this.retryCount + 1;
        this.lastTriedAt = LocalDateTime.now();
        this.lastErrorMessage = errorMessage;
    }
}
