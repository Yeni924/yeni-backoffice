package com.yeni.backoffice.core.commerce.service;

import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderCreateRequest;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderResponse;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderSummaryResponse;
import com.yeni.backoffice.core.commerce.entity.CommerceOrder;
import com.yeni.backoffice.core.commerce.enums.OrderPaymentStatus;
import com.yeni.backoffice.core.commerce.enums.OrderStatus;
import com.yeni.backoffice.core.commerce.repository.CommerceOrderRepository;
import com.yeni.backoffice.core.common.exception.BusinessException;
import com.yeni.backoffice.core.common.exception.ConflictException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.service.PaymentApproveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CommerceOrderService {

    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String DEFAULT_CHANNEL_TYPE = "WEB";
    private static final String DEFAULT_STORE_CODE = "PORTFOLIO";
    private static final String DEFAULT_PAYMENT_METHOD = "CARD";

    private final CommerceOrderRepository orderRepository;
    private final PaymentApproveService paymentApproveService;

    public CommerceOrderService(
            CommerceOrderRepository orderRepository,
            PaymentApproveService paymentApproveService) {
        this.orderRepository = orderRepository;
        this.paymentApproveService = paymentApproveService;
    }

    @Transactional
    public CommerceOrderResponse createOrder(CommerceOrderCreateRequest request) {
        validateCreateRequest(request);
        String orderNo = StringUtils.hasText(request.orderNo())
                ? request.orderNo().trim()
                : "ORDER-COMMERCE-" + System.currentTimeMillis();
        orderRepository.findByOrderNo(orderNo)
                .ifPresent(order -> {
                    throw new ConflictException(ErrorCode.CONFLICT, "이미 생성된 주문번호입니다.");
                });

        CommerceOrder order = CommerceOrder.builder()
                .orderNo(orderNo)
                .buyerName(request.buyerName().trim())
                .productName(request.productName().trim())
                .orderAmount(request.orderAmount())
                .orderStatus(OrderStatus.CREATED)
                .paymentStatus(OrderPaymentStatus.READY)
                .lastMessage("주문이 생성되었습니다.")
                .build();
        return CommerceOrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public CommerceOrderResponse createMockOrder() {
        return createOrder(new CommerceOrderCreateRequest(
                null,
                "포트폴리오 고객",
                "커머스 주문 테스트 상품",
                BigDecimal.valueOf(12000)
        ));
    }

    @Transactional
    public CommerceOrderResponse approvePayment(Long orderId) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (order.getPaymentId() != null || OrderStatus.PAID.equals(order.getOrderStatus())) {
            throw new ConflictException(ErrorCode.CONFLICT, "이미 결제 처리된 주문입니다.");
        }

        PaymentApproveResponse response;
        try {
            response = paymentApproveService.approvePayment(new PaymentApproveRequest(
                    PgProvider.MOCK,
                    order.getOrderNo(),
                    order.getOrderAmount(),
                    DEFAULT_CURRENCY,
                    order.getBuyerName(),
                    order.getProductName(),
                    "ORDER-PAY-" + order.getOrderNo(),
                    DEFAULT_CHANNEL_TYPE,
                    DEFAULT_STORE_CODE,
                    DEFAULT_PAYMENT_METHOD
            ));
        } catch (BusinessException exception) {
            order.markPaymentResult(null, null, OrderPaymentStatus.FAILED, OrderStatus.CREATED, exception.getMessage());
            throw exception;
        }

        OrderPaymentStatus paymentStatus = "APPROVED".equals(response.paymentStatus())
                ? OrderPaymentStatus.APPROVED
                : "APPROVE_UNKNOWN".equals(response.paymentStatus())
                ? OrderPaymentStatus.APPROVE_UNKNOWN
                : OrderPaymentStatus.FAILED;
        OrderStatus orderStatus = OrderPaymentStatus.APPROVED.equals(paymentStatus) ? OrderStatus.PAID : OrderStatus.CREATED;
        order.markPaymentResult(response.paymentId(), response.tid(), paymentStatus, orderStatus, response.resultMessage());
        return CommerceOrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<CommerceOrderResponse> getOrders() {
        return orderRepository.findAllByOrderByIdDesc().stream()
                .map(CommerceOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommerceOrderSummaryResponse getSummary() {
        return CommerceOrderSummaryResponse.from(orderRepository.findAll());
    }

    private void validateCreateRequest(CommerceOrderCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.buyerName())
                || !StringUtils.hasText(request.productName())
                || request.orderAmount() == null
                || request.orderAmount().signum() <= 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "구매자명, 상품명, 0보다 큰 주문금액은 필수입니다.");
        }
    }
}
