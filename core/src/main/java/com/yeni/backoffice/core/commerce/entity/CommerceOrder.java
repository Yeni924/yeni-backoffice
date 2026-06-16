package com.yeni.backoffice.core.commerce.entity;

import com.yeni.backoffice.core.commerce.enums.OrderPaymentStatus;
import com.yeni.backoffice.core.commerce.enums.OrderStatus;
import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "commerce_order",
        uniqueConstraints = @UniqueConstraint(name = "uk_commerce_order_no", columnNames = "orderNo")
)
public class CommerceOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String orderNo;

    @Column(nullable = false, length = 80)
    private String buyerName;

    @Column(nullable = false, length = 160)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderPaymentStatus paymentStatus;

    private Long paymentId;

    @Column(length = 120)
    private String tid;

    @Column(length = 200)
    private String lastMessage;

    public void markPaymentResult(Long paymentId, String tid, OrderPaymentStatus paymentStatus, OrderStatus orderStatus, String message) {
        this.paymentId = paymentId;
        this.tid = tid;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.lastMessage = message;
    }
}
