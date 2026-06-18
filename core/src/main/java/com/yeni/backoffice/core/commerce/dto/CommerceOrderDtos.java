package com.yeni.backoffice.core.commerce.dto;

import com.yeni.backoffice.core.commerce.entity.CommerceOrder;
import com.yeni.backoffice.core.commerce.entity.CommerceOrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CommerceOrderDtos {

    private CommerceOrderDtos() {
    }

    public record CommerceOrderCreateRequest(
            String orderNo,
            @NotBlank(message = "구매자명은 필수입니다.")
            String buyerName,
            String buyerPhone,
            @NotNull(message = "배송비는 필수입니다.")
            @PositiveOrZero(message = "배송비는 0 이상이어야 합니다.")
            BigDecimal deliveryFee,
            @NotNull(message = "할인금액은 필수입니다.")
            @PositiveOrZero(message = "할인금액은 0 이상이어야 합니다.")
            BigDecimal discountAmount,
            @Valid
            @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
            List<CommerceOrderItemCreateRequest> items
    ) {
    }

    public record CommerceOrderItemCreateRequest(
            @NotBlank(message = "상품코드는 필수입니다.")
            String productCode,
            @NotBlank(message = "상품명은 필수입니다.")
            String productName,
            String optionName,
            @NotNull(message = "상품 단가는 필수입니다.")
            @Positive(message = "상품 단가는 0보다 커야 합니다.")
            BigDecimal unitPrice,
            @Positive(message = "상품 수량은 1 이상이어야 합니다.")
            int quantity
    ) {
    }

    public record CommerceOrderResponse(
            Long id,
            String orderNo,
            String buyerName,
            String buyerPhone,
            String productName,
            int itemCount,
            BigDecimal productAmount,
            BigDecimal deliveryFee,
            BigDecimal discountAmount,
            BigDecimal payableAmount,
            String orderStatus,
            String paymentStatus,
            Long paymentId,
            String tid,
            String lastMessage,
            List<CommerceOrderItemResponse> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CommerceOrderResponse from(CommerceOrder order, List<CommerceOrderItem> items) {
            return new CommerceOrderResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getBuyerName(),
                    order.getBuyerPhone(),
                    order.getProductName(),
                    items.size(),
                    order.getProductAmount(),
                    order.getDeliveryFee(),
                    order.getDiscountAmount(),
                    order.getPayableAmount(),
                    order.getOrderStatus().name(),
                    order.getPaymentStatus().name(),
                    order.getPaymentId(),
                    order.getTid(),
                    order.getLastMessage(),
                    items.stream().map(CommerceOrderItemResponse::from).toList(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        }
    }

    public record CommerceOrderItemResponse(
            Long id,
            String productCode,
            String productName,
            String optionName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal itemAmount
    ) {
        public static CommerceOrderItemResponse from(CommerceOrderItem item) {
            return new CommerceOrderItemResponse(
                    item.getId(),
                    item.getProductCode(),
                    item.getProductName(),
                    item.getOptionName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getItemAmount()
            );
        }
    }

    public record CommerceOrderSummaryResponse(
            long totalCount,
            long paidCount,
            long paymentReadyCount,
            long paymentUnknownCount,
            BigDecimal totalOrderAmount
    ) {
        public static CommerceOrderSummaryResponse from(List<CommerceOrder> orders) {
            return new CommerceOrderSummaryResponse(
                    orders.size(),
                    orders.stream().filter(order -> "PAID".equals(order.getOrderStatus().name())).count(),
                    orders.stream().filter(order -> "READY".equals(order.getPaymentStatus().name())).count(),
                    orders.stream().filter(order -> "APPROVE_UNKNOWN".equals(order.getPaymentStatus().name())).count(),
                    orders.stream()
                            .map(CommerceOrder::getPayableAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        }
    }
}
