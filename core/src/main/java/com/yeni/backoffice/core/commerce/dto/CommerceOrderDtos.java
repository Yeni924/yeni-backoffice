package com.yeni.backoffice.core.commerce.dto;

import com.yeni.backoffice.core.commerce.entity.CommerceOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
            @NotBlank(message = "상품명은 필수입니다.")
            String productName,
            @NotNull(message = "주문금액은 필수입니다.")
            @Positive(message = "주문금액은 0보다 커야 합니다.")
            BigDecimal orderAmount
    ) {
    }

    public record CommerceOrderResponse(
            Long id,
            String orderNo,
            String buyerName,
            String productName,
            BigDecimal orderAmount,
            String orderStatus,
            String paymentStatus,
            Long paymentId,
            String tid,
            String lastMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static CommerceOrderResponse from(CommerceOrder order) {
            return new CommerceOrderResponse(
                    order.getId(),
                    order.getOrderNo(),
                    order.getBuyerName(),
                    order.getProductName(),
                    order.getOrderAmount(),
                    order.getOrderStatus().name(),
                    order.getPaymentStatus().name(),
                    order.getPaymentId(),
                    order.getTid(),
                    order.getLastMessage(),
                    order.getCreatedAt(),
                    order.getUpdatedAt()
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
                            .map(CommerceOrder::getOrderAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        }
    }
}
