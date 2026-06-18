package com.yeni.backoffice.core.commerce.service;

import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderCreateRequest;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderItemCreateRequest;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderResponse;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderSummaryResponse;
import com.yeni.backoffice.core.commerce.entity.CommerceOrder;
import com.yeni.backoffice.core.commerce.entity.CommerceOrderItem;
import com.yeni.backoffice.core.commerce.enums.OrderPaymentStatus;
import com.yeni.backoffice.core.commerce.enums.OrderStatus;
import com.yeni.backoffice.core.commerce.repository.CommerceOrderItemRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CommerceOrderService {

    private static final String DEFAULT_CURRENCY = "KRW";
    private static final String DEFAULT_CHANNEL_TYPE = "WEB";
    private static final String DEFAULT_STORE_CODE = "PORTFOLIO";
    private static final String DEFAULT_PAYMENT_METHOD = "CARD";

    private final CommerceOrderRepository orderRepository;
    private final CommerceOrderItemRepository orderItemRepository;
    private final PaymentApproveService paymentApproveService;

    public CommerceOrderService(
            CommerceOrderRepository orderRepository,
            CommerceOrderItemRepository orderItemRepository,
            PaymentApproveService paymentApproveService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentApproveService = paymentApproveService;
    }

    @Transactional
    public CommerceOrderResponse createOrder(CommerceOrderCreateRequest request) {
        validateCreateRequest(request);
        String orderNo = StringUtils.hasText(request.orderNo()) ? request.orderNo().trim() : generateOrderNo();
        orderRepository.findByOrderNo(orderNo)
                .ifPresent(order -> {
                    throw new ConflictException(ErrorCode.CONFLICT, "이미 생성된 주문번호입니다.");
                });

        BigDecimal productAmount = calculateProductAmount(request.items());
        BigDecimal deliveryFee = request.deliveryFee();
        BigDecimal discountAmount = request.discountAmount();
        String representativeProductName = representativeProductName(request.items());

        CommerceOrder order = CommerceOrder.builder()
                .orderNo(orderNo)
                .buyerName(request.buyerName().trim())
                .buyerPhone(trimToNull(request.buyerPhone()))
                .productName(representativeProductName)
                .productAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .payableAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatus.CREATED)
                .paymentStatus(OrderPaymentStatus.READY)
                .lastMessage("주문이 생성되었습니다.")
                .build();
        order.recalculateAmounts(productAmount, deliveryFee, discountAmount);
        CommerceOrder savedOrder = orderRepository.save(order);

        List<CommerceOrderItem> savedItems = request.items().stream()
                .map(item -> CommerceOrderItem.create(
                        savedOrder.getId(),
                        item.productCode().trim(),
                        item.productName().trim(),
                        trimToNull(item.optionName()),
                        item.unitPrice(),
                        item.quantity()))
                .map(orderItemRepository::save)
                .toList();

        return CommerceOrderResponse.from(savedOrder, savedItems);
    }

    @Transactional
    public CommerceOrderResponse createMockOrder() {
        return createOrder(new CommerceOrderCreateRequest(
                null,
                "포트폴리오 고객",
                "010-0000-0000",
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(2000),
                List.of(
                        new CommerceOrderItemCreateRequest("K2-DEMO-001", "커머스 주문 테스트 상품", "블랙 / 095", BigDecimal.valueOf(10000), 2),
                        new CommerceOrderItemCreateRequest("K2-DEMO-002", "추가 구성 상품", "기본", BigDecimal.valueOf(5000), 1)
                )
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
                    order.getPayableAmount(),
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
        return CommerceOrderResponse.from(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
    }

    @Transactional(readOnly = true)
    public List<CommerceOrderResponse> getOrders() {
        return orderRepository.findAllByOrderByIdDesc().stream()
                .map(order -> CommerceOrderResponse.from(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommerceOrderSummaryResponse getSummary() {
        return CommerceOrderSummaryResponse.from(orderRepository.findAll());
    }

    private void validateCreateRequest(CommerceOrderCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.buyerName())) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "구매자명은 필수입니다.");
        }
        if (request.deliveryFee() == null || request.deliveryFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "배송비는 0 이상이어야 합니다.");
        }
        if (request.discountAmount() == null || request.discountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "할인금액은 0 이상이어야 합니다.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "주문 상품은 1개 이상이어야 합니다.");
        }
    }

    private BigDecimal calculateProductAmount(List<CommerceOrderItemCreateRequest> items) {
        return items.stream()
                .map(item -> {
                    if (!StringUtils.hasText(item.productCode()) || !StringUtils.hasText(item.productName())) {
                        throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "상품코드와 상품명은 필수입니다.");
                    }
                    return CommerceOrderItem.create(
                                    0L,
                                    item.productCode().trim(),
                                    item.productName().trim(),
                                    trimToNull(item.optionName()),
                                    item.unitPrice(),
                                    item.quantity())
                            .getItemAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String representativeProductName(List<CommerceOrderItemCreateRequest> items) {
        String firstName = items.get(0).productName().trim();
        return items.size() == 1 ? firstName : firstName + " 외 " + (items.size() - 1) + "건";
    }

    private String generateOrderNo() {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
