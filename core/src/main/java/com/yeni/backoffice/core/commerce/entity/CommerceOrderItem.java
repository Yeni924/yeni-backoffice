package com.yeni.backoffice.core.commerce.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "commerce_order_item")
public class CommerceOrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 80)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 200)
    private String optionName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal itemAmount;

    public static CommerceOrderItem create(
            Long orderId,
            String productCode,
            String productName,
            String optionName,
            BigDecimal unitPrice,
            int quantity) {
        validate(unitPrice, quantity);

        CommerceOrderItem item = new CommerceOrderItem();
        item.orderId = orderId;
        item.productCode = productCode;
        item.productName = productName;
        item.optionName = optionName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.itemAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    private static void validate(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "상품 단가는 0보다 커야 합니다.");
        }
        if (quantity <= 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "상품 수량은 1 이상이어야 합니다.");
        }
    }
}
