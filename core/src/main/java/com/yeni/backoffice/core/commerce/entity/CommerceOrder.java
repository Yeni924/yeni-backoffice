package com.yeni.backoffice.core.commerce.entity;

import com.yeni.backoffice.core.commerce.enums.OrderPaymentStatus;
import com.yeni.backoffice.core.commerce.enums.OrderStatus;
import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
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

    @Column(length = 40)
    private String buyerPhone;

    @Column(nullable = false, length = 160)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal productAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal payableAmount;

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

    public void recalculateAmounts(
            BigDecimal productAmount,
            BigDecimal deliveryFee,
            BigDecimal discountAmount) {
        validateNonNegative(productAmount, "상품합계는 0 이상이어야 합니다.");
        validateNonNegative(deliveryFee, "배송비는 0 이상이어야 합니다.");
        validateNonNegative(discountAmount, "할인금액은 0 이상이어야 합니다.");

        BigDecimal calculatedPayableAmount = productAmount.add(deliveryFee).subtract(discountAmount);
        if (calculatedPayableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "최종 결제금액은 0보다 커야 합니다.");
        }
        if (discountAmount.compareTo(productAmount.add(deliveryFee)) > 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "할인금액은 상품합계와 배송비 합계를 초과할 수 없습니다.");
        }

        this.productAmount = productAmount;
        this.deliveryFee = deliveryFee;
        this.discountAmount = discountAmount;
        this.payableAmount = calculatedPayableAmount;
    }

    public void markPaymentResult(Long paymentId, String tid, OrderPaymentStatus paymentStatus, OrderStatus orderStatus, String message) {
        this.paymentId = paymentId;
        this.tid = tid;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.lastMessage = message;
    }

    private void validateNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
    }
}
