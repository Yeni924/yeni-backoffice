package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.CancelStatus;
import com.yeni.backoffice.core.payment.enums.CancelType;
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
        name = "payment_cancel",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_cancel_request_key", columnNames = "cancelRequestKey")
)
public class PaymentCancel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 120)
    private String tid;

    @Column(nullable = false, length = 100)
    private String cancelRequestKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cancelAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancelType cancelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CancelStatus cancelStatus;

    @Column(nullable = false)
    private LocalDateTime canceledAt;

    @Column(length = 200)
    private String cancelReason;
}
