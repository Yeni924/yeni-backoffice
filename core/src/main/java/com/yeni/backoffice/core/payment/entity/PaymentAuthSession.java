package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.PaymentAuthStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "payment_auth_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_auth_session_order_no", columnNames = "orderNo"),
                @UniqueConstraint(name = "uk_payment_auth_session_auth_token", columnNames = "authToken")
        }
)
public class PaymentAuthSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String mid;

    @Column(nullable = false, length = 80)
    private String orderNo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 120)
    private String buyerName;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, length = 80)
    private String authToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentAuthStatus status;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(length = 120)
    private String approvedTid;

    public void markAuthResultReceived() {
        this.status = PaymentAuthStatus.AUTH_RESULT_RECEIVED;
    }

    public void markApproved(String approvedTid) {
        this.status = PaymentAuthStatus.APPROVED;
        this.approvedTid = approvedTid;
    }

    public void markFailed() {
        this.status = PaymentAuthStatus.FAILED;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiredAt.isBefore(now);
    }
}
