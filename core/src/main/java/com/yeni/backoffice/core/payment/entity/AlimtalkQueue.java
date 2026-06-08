package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
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
        name = "alimtalk_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_alimtalk_queue_message_key", columnNames = "messageKey")
)
public class AlimtalkQueue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    private Long salesId;

    @Column(nullable = false, length = 120)
    private String messageKey;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlimtalkStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(length = 300)
    private String lastErrorMessage;

    private LocalDateTime sentAt;

    public void markFailed(String errorMessage) {
        this.status = AlimtalkStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.lastErrorMessage = errorMessage;
        this.sentAt = LocalDateTime.now();
    }

    public void markRetryReady() {
        this.status = AlimtalkStatus.RETRY_READY;
    }

    public void markSuccess() {
        this.status = AlimtalkStatus.SUCCESS;
        this.lastErrorMessage = null;
        this.sentAt = LocalDateTime.now();
    }
}
