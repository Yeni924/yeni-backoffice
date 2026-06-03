package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
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
        name = "external_send_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_external_send_request_key", columnNames = "requestKey")
)
public class ExternalSendRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long salesId;

    @Column(nullable = false, length = 100)
    private String requestKey;

    @Column(nullable = false, length = 40)
    private String targetSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExternalSendStatus sendStatus;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(length = 200)
    private String lastErrorMessage;

    private LocalDateTime lastSentAt;

    public void markSuccess() {
        this.sendStatus = ExternalSendStatus.SUCCESS;
        this.lastSentAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.sendStatus = ExternalSendStatus.FAILED;
        this.retryCount = this.retryCount + 1;
        this.lastSentAt = LocalDateTime.now();
        this.lastErrorMessage = errorMessage;
    }
}
