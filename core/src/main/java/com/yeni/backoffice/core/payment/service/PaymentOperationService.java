package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.payment.adapter.PaymentGatewayAdapter;
import com.yeni.backoffice.core.payment.adapter.PaymentGatewayAdapterResolver;
import com.yeni.backoffice.core.payment.config.InicisStdPayProperties;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisAuthResultRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisReadyRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisReadyResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgLogResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesAdjustmentRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentQueryResponse;
import com.yeni.backoffice.core.payment.entity.AuditLog;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentAuthSession;
import com.yeni.backoffice.core.payment.entity.PaymentCancel;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.PgApiLog;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementAdjustment;
import com.yeni.backoffice.core.payment.enums.CancelStatus;
import com.yeni.backoffice.core.payment.enums.CancelType;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.enums.LogResultStatus;
import com.yeni.backoffice.core.payment.enums.PaymentAuthStatus;
import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PaymentEventType;
import com.yeni.backoffice.core.payment.enums.PgApiType;
import com.yeni.backoffice.core.payment.enums.PgCompany;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.enums.SaleStatus;
import com.yeni.backoffice.core.payment.enums.SaleType;
import com.yeni.backoffice.core.payment.repository.AuditLogRepository;
import com.yeni.backoffice.core.payment.repository.ExternalSendRequestRepository;
import com.yeni.backoffice.core.payment.repository.PaymentAuthSessionRepository;
import com.yeni.backoffice.core.payment.repository.PaymentCancelRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.PgApiLogRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementAdjustmentRepository;
import com.yeni.backoffice.core.payment.util.InicisSignatureService;
import com.yeni.backoffice.core.payment.gateway.PaymentGateway;
import com.yeni.backoffice.core.payment.gateway.PaymentGatewayRegistry;
import com.yeni.backoffice.core.payment.gateway.PaymentGatewayRouter;
import com.yeni.backoffice.core.payment.gateway.command.PaymentApproveCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentCancelCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentQueryCommand;
import com.yeni.backoffice.core.payment.gateway.result.PaymentApproveResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentCancelResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentQueryResult;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentOperationService {

    private static final PgCompany PG_COMPANY = PgCompany.INICIS;
    private static final String PAYMENT_METHOD = "CARD";

    private final InicisStdPayProperties inicisProperties;
    private final InicisSignatureService signatureService;
    private final PaymentGatewayAdapterResolver adapterResolver;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentAuthSessionRepository authSessionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final PgApiLogRepository pgApiLogRepository;
    private final SalesTransactionRepository salesRepository;
    private final ExternalSendRequestRepository externalSendRequestRepository;
    private final SettlementAdjustmentRepository adjustmentRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentOperationService(
            InicisStdPayProperties inicisProperties,
            InicisSignatureService signatureService,
            PaymentGatewayAdapterResolver adapterResolver,
            PaymentGatewayRegistry gatewayRegistry,
            PaymentGatewayRouter gatewayRouter,
            PaymentAuthSessionRepository authSessionRepository,
            PaymentTransactionRepository paymentRepository,
            PaymentCancelRepository cancelRepository,
            PgApiLogRepository pgApiLogRepository,
            SalesTransactionRepository salesRepository,
            ExternalSendRequestRepository externalSendRequestRepository,
            SettlementAdjustmentRepository adjustmentRepository,
            AuditLogRepository auditLogRepository) {
        this.inicisProperties = inicisProperties;
        this.signatureService = signatureService;
        this.adapterResolver = adapterResolver;
        this.gatewayRegistry = gatewayRegistry;
        this.gatewayRouter = gatewayRouter;
        this.authSessionRepository = authSessionRepository;
        this.paymentRepository = paymentRepository;
        this.cancelRepository = cancelRepository;
        this.pgApiLogRepository = pgApiLogRepository;
        this.salesRepository = salesRepository;
        this.externalSendRequestRepository = externalSendRequestRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public PaymentApproveResponse approvePayment(PaymentApproveRequest request) {
        validateBridgeApproveRequest(request);
        paymentRepository.findByOrderNo(request.orderNo())
                .ifPresent(payment -> {
                    throw new IllegalArgumentException("Order already has a payment transaction.");
                });

        PgProvider provider = gatewayRouter.route(
                request.pgProvider(),
                request.channelType(),
                request.storeCode(),
                defaultText(request.paymentMethod(), PAYMENT_METHOD)
        );
        String idempotencyKey = defaultText(request.idempotencyKey(), "APPROVE-" + request.orderNo());
        PaymentGateway gateway = gatewayRegistry.get(provider);
        PaymentApproveCommand command = new PaymentApproveCommand(
                provider,
                midByProvider(provider),
                request.orderNo(),
                request.amount(),
                defaultCurrency(request.currency()),
                idempotencyKey,
                defaultText(request.channelType(), "WEB"),
                defaultText(request.storeCode(), "PORTFOLIO"),
                defaultText(request.paymentMethod(), PAYMENT_METHOD)
        );

        long startedAt = System.currentTimeMillis();
        PgApiLog apiLog = savePgLog(null, request.orderNo(), provider, PaymentEventType.APPROVE, PgApiType.APPROVE,
                idempotencyKey, command.toString(), LogResultStatus.REQUESTED, "PGB approve requested");
        PaymentApproveResult result = gateway.approve(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage());

        if (result.unknown()) {
            PaymentTransaction payment = saveUnknownPayment(command, result);
            saveAudit("PAYMENT", "UNKNOWN", request.orderNo(), "PGB approve result is unknown. retry-query is required.");
            return toApproveResponse(payment, provider, result.resultCode(), result.resultMessage());
        }
        if (!result.success()) {
            saveAudit("PAYMENT", "APPROVE_FAILED", request.orderNo(), result.resultMessage());
            throw new IllegalArgumentException("Payment approve failed: " + result.resultMessage());
        }

        PaymentTransaction payment = PaymentTransaction.builder()
                .mid(command.mid())
                .pgProvider(provider)
                .orderNo(command.orderNo())
                .tid(result.tid())
                .approvedAmount(command.amount())
                .canceledAmount(BigDecimal.ZERO)
                .currency(command.currency())
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(result.approvedAt())
                .build();
        paymentRepository.save(payment);
        apiLog.complete(result.toString(), LogResultStatus.SUCCESS, result.resultMessage());

        SalesTransaction sales = createSales(payment, SaleType.SALE, payment.getId(), payment.getApprovedAmount(), result.approvedAt());
        createExternalSendRequest(sales, "SALE-" + payment.getOrderNo());
        saveAudit("PAYMENT", "APPROVED", payment.getOrderNo(), "PGB approve completed through " + provider);

        return toApproveResponse(payment, provider, result.resultCode(), result.resultMessage() + " (" + (System.currentTimeMillis() - startedAt) + "ms)");
    }

    @Transactional
    public PaymentBridgeCancelResponse cancelPaymentBridge(Long paymentId, PaymentBridgeCancelRequest request) {
        validateBridgeCancelRequest(request);
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        String idempotencyKey = defaultText(request.idempotencyKey(), "CANCEL-" + payment.getOrderNo() + "-" + System.currentTimeMillis());
        cancelRepository.findByCancelRequestKey(idempotencyKey)
                .ifPresent(cancel -> {
                    throw new IllegalArgumentException("Cancel idempotency key already exists.");
                });
        if (payment.isCancelCompleted()) {
            throw new IllegalArgumentException("Payment is already fully canceled.");
        }
        if (request.cancelAmount().compareTo(payment.getCancelableAmount()) > 0) {
            throw new IllegalArgumentException("Cancel amount exceeds cancelable amount.");
        }

        PgProvider provider = request.pgProvider() == null ? payment.getPgProvider() : request.pgProvider();
        PaymentGateway gateway = gatewayRegistry.get(provider);
        PaymentCancelCommand command = new PaymentCancelCommand(provider, payment.getTid(), request.cancelAmount(), idempotencyKey, request.cancelReason());
        PgApiLog apiLog = savePgLog(payment.getId(), payment.getOrderNo(), provider, PaymentEventType.CANCEL, PgApiType.CANCEL,
                idempotencyKey, command.toString(), LogResultStatus.REQUESTED, "PGB cancel requested");
        PaymentCancelResult result = gateway.cancel(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage());

        if (result.unknown()) {
            payment.updateStatus(PaymentStatus.UNKNOWN, result.resultMessage());
            saveAudit("PAYMENT", "CANCEL_UNKNOWN", payment.getOrderNo(), result.resultMessage());
            throw new IllegalArgumentException("Payment cancel result is unknown. retry-query is required.");
        }
        if (!result.success()) {
            payment.updateStatus(PaymentStatus.CANCEL_FAILED, result.resultMessage());
            saveAudit("PAYMENT", "CANCEL_FAILED", payment.getOrderNo(), result.resultMessage());
            throw new IllegalArgumentException("Payment cancel failed: " + result.resultMessage());
        }

        CancelType cancelType = request.cancelAmount().compareTo(payment.getCancelableAmount()) == 0 ? CancelType.FULL : CancelType.PARTIAL;
        payment.cancel(request.cancelAmount());
        PaymentCancel cancel = PaymentCancel.builder()
                .paymentId(payment.getId())
                .tid(payment.getTid())
                .cancelRequestKey(idempotencyKey)
                .cancelAmount(request.cancelAmount())
                .cancelType(cancelType)
                .cancelStatus(CancelStatus.SUCCESS)
                .canceledAt(LocalDateTime.now())
                .cancelReason(request.cancelReason())
                .build();
        cancelRepository.save(cancel);
        SalesTransaction sales = createSales(payment, SaleType.CANCEL, cancel.getId(), request.cancelAmount().negate(), cancel.getCanceledAt());
        createExternalSendRequest(sales, "CANCEL-" + idempotencyKey);
        saveAudit("PAYMENT", "CANCELED", payment.getOrderNo(), "PGB cancel completed through " + provider);

        return new PaymentBridgeCancelResponse(cancel.getId(), payment.getId(), provider, payment.getTid(), cancel.getCancelAmount(),
                cancel.getCancelType().name(), payment.getPaymentStatus().name(), result.resultCode(), result.resultMessage());
    }

    @Transactional
    public PaymentQueryResponse retryQuery(Long paymentId) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        PaymentGateway gateway = gatewayRegistry.get(payment.getPgProvider());
        PaymentQueryCommand command = new PaymentQueryCommand(payment.getPgProvider(), payment.getTid(), payment.getOrderNo());
        PgApiLog apiLog = savePgLog(payment.getId(), payment.getOrderNo(), payment.getPgProvider(), PaymentEventType.QUERY,
                PgApiType.APPROVE, null, command.toString(), LogResultStatus.REQUESTED, "PGB query requested");
        PaymentQueryResult result = gateway.query(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage());
        if (result.success() && PaymentStatus.UNKNOWN.equals(payment.getPaymentStatus())) {
            payment.updateStatus(result.paymentStatus(), null);
        }
        return PaymentQueryResponse.from(payment, payment.getPgProvider(), result.resultCode(), result.resultMessage());
    }

    @Transactional
    public InicisReadyResponse prepareStdPay(InicisReadyRequest request) {
        validateReadyRequest(request);
        authSessionRepository.findByOrderNo(request.orderNo())
                .ifPresent(session -> {
                    throw new IllegalArgumentException("Order number already has a payment session.");
                });

        String authToken = UUID.randomUUID().toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = signatureService.createReadySignature(request.orderNo(), request.amount(), timestamp);

        PaymentAuthSession session = PaymentAuthSession.builder()
                .mid(inicisProperties.getMid())
                .orderNo(request.orderNo())
                .amount(request.amount())
                .currency(defaultCurrency(request.currency()))
                .buyerName(request.buyerName().trim())
                .productName(request.productName().trim())
                .authToken(authToken)
                .status(PaymentAuthStatus.REQUEST_READY)
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .build();
        authSessionRepository.save(session);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "1.0");
        params.put("gopaymethod", "Card");
        params.put("mid", inicisProperties.getMid());
        params.put("oid", request.orderNo());
        params.put("price", request.amount().stripTrailingZeros().toPlainString());
        params.put("timestamp", timestamp);
        params.put("signature", signature);
        params.put("mKey", signatureService.createMKey());
        params.put("returnUrl", inicisProperties.getReturnUrl());
        params.put("closeUrl", inicisProperties.getCloseUrl());

        savePgLog(null, request.orderNo(), PgApiType.STD_PAY_READY, params.toString(), LogResultStatus.READY, "표준결제 요청 파라미터 생성");
        saveAudit("PAYMENT", "READY", request.orderNo(), "Inicis StdPay mock 결제 준비 요청을 생성했습니다.");

        return new InicisReadyResponse(
                inicisProperties.getMid(),
                request.orderNo(),
                request.amount(),
                defaultCurrency(request.currency()),
                authToken,
                signature,
                signatureService.createMKey(),
                inicisProperties.getReturnUrl(),
                inicisProperties.getCloseUrl(),
                params
        );
    }

    @Transactional
    public InicisApproveResponse handleStdPayReturn(InicisAuthResultRequest request) {
        validateAuthResultRequest(request);
        PaymentAuthSession session = authSessionRepository.findByAuthToken(request.authToken())
                .orElseThrow(() -> new IllegalArgumentException("Payment auth session not found."));
        session.markAuthResultReceived();
        savePgLog(null, request.orderNo(), PgApiType.AUTH_RESULT, request.toString(), LogResultStatus.RECEIVED, request.resultMessage());

        if (session.isExpired(LocalDateTime.now())) {
            session.markFailed();
            throw new IllegalArgumentException("Payment auth session is expired.");
        }
        if (!"0000".equals(request.resultCode())) {
            session.markFailed();
            throw new IllegalArgumentException("PG auth result failed: " + request.resultMessage());
        }
        validateAuthMatchesSession(request, session);

        PaymentGatewayAdapter adapter = adapterResolver.resolve(PG_COMPANY);
        PaymentGatewayAdapter.ApprovalResult approvalResult = adapter.approve(
                new PaymentGatewayAdapter.ApprovalRequest(
                        request.mid(),
                        request.orderNo(),
                        request.amount(),
                        request.authToken()
                )
        );
        PgApiLog approvalLog = savePgLog(null, request.orderNo(), PgApiType.APPROVE, request.toString(), LogResultStatus.REQUESTED, "승인 mock 요청");

        if (!approvalResult.success()) {
            approvalLog.complete(approvalResult.toString(), LogResultStatus.FAILED, approvalResult.resultMessage());
            session.markFailed();
            saveAudit("PAYMENT", "APPROVE_FAILED", request.orderNo(), approvalResult.resultMessage());
            throw new IllegalArgumentException("PG approve failed: " + approvalResult.resultMessage());
        }

        try {
            validateApprovalResult(session, approvalResult);
            PaymentTransaction payment = PaymentTransaction.builder()
                    .mid(session.getMid())
                    .orderNo(session.getOrderNo())
                    .tid(approvalResult.tid())
                    .approvedAmount(session.getAmount())
                    .canceledAmount(BigDecimal.ZERO)
                    .currency(session.getCurrency())
                    .paymentStatus(PaymentStatus.APPROVED)
                    .approvedAt(approvalResult.approvedAt())
                    .build();
            paymentRepository.save(payment);
            session.markApproved(approvalResult.tid());

            SalesTransaction sales = createSales(payment, SaleType.SALE, payment.getId(), payment.getApprovedAmount(), approvalResult.approvedAt());
            createExternalSendRequest(sales, "SALE-" + payment.getOrderNo());

            approvalLog.complete(approvalResult.toString(), LogResultStatus.SUCCESS, approvalResult.resultMessage());
            saveAudit("PAYMENT", "APPROVED", payment.getOrderNo(), "PG 승인 완료 후 매출 및 외부 전송 요청을 생성했습니다.");

            return new InicisApproveResponse(
                    payment.getId(),
                    payment.getOrderNo(),
                    payment.getTid(),
                    payment.getPaymentStatus().name(),
                    payment.getApprovedAmount(),
                    payment.getApprovedAt()
            );
        } catch (RuntimeException e) {
            adapter.netCancel(new PaymentGatewayAdapter.NetCancelRequest(approvalResult.tid(), e.getMessage()));
            approvalLog.complete(approvalResult.toString(), LogResultStatus.NET_CANCEL_REQUESTED, e.getMessage());
            saveAudit("PAYMENT", "NET_CANCEL_REQUESTED", request.orderNo(), "승인 후 내부 처리 실패로 netCancel 보상 흐름을 수행했습니다.");
            throw e;
        }
    }

    @Transactional
    public PaymentCancelResponse cancelPayment(Long paymentId, PaymentCancelRequest request) {
        validateCancelRequest(request);
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        cancelRepository.findByCancelRequestKey(request.cancelRequestKey())
                .ifPresent(cancel -> {
                    throw new IllegalArgumentException("Cancel request key already exists.");
                });

        if (payment.isCancelCompleted()) {
            throw new IllegalArgumentException("Payment is already fully canceled.");
        }
        if (request.cancelAmount().compareTo(payment.getCancelableAmount()) > 0) {
            throw new IllegalArgumentException("Cancel amount exceeds cancelable amount.");
        }

        PaymentGatewayAdapter adapter = adapterResolver.resolve(PG_COMPANY);
        PaymentGatewayAdapter.CancelResult result = adapter.cancel(new PaymentGatewayAdapter.CancelRequest(
                payment.getTid(),
                request.cancelAmount(),
                request.cancelRequestKey()
        ));
        PgApiLog cancelLog = savePgLog(payment.getId(), payment.getOrderNo(), PgApiType.CANCEL, request.toString(), LogResultStatus.REQUESTED, "취소 mock 요청");
        if (!result.success()) {
            cancelLog.complete(result.toString(), LogResultStatus.FAILED, result.resultMessage());
            throw new IllegalArgumentException("PG cancel failed: " + result.resultMessage());
        }

        CancelType cancelType = request.cancelAmount().compareTo(payment.getCancelableAmount()) == 0 ? CancelType.FULL : CancelType.PARTIAL;
        payment.cancel(request.cancelAmount());
        PaymentCancel cancel = PaymentCancel.builder()
                .paymentId(payment.getId())
                .tid(payment.getTid())
                .cancelRequestKey(request.cancelRequestKey())
                .cancelAmount(request.cancelAmount())
                .cancelType(cancelType)
                .cancelStatus(CancelStatus.SUCCESS)
                .canceledAt(LocalDateTime.now())
                .cancelReason(request.cancelReason())
                .build();
        cancelRepository.save(cancel);

        SalesTransaction sales = createSales(payment, SaleType.CANCEL, cancel.getId(), request.cancelAmount().negate(), cancel.getCanceledAt());
        createExternalSendRequest(sales, "CANCEL-" + request.cancelRequestKey());
        cancelLog.complete(result.toString(), LogResultStatus.SUCCESS, result.resultMessage());
        saveAudit("PAYMENT", "CANCELED", payment.getOrderNo(), "PG 취소 완료 후 취소 매출 및 외부 전송 요청을 생성했습니다.");

        return new PaymentCancelResponse(
                cancel.getId(),
                payment.getId(),
                payment.getTid(),
                cancel.getCancelAmount(),
                cancel.getCancelType().name(),
                payment.getPaymentStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments() {
        return paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "approvedAt")).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
    }

    @Transactional(readOnly = true)
    public List<PgLogResponse> getPaymentLogs(Long paymentId) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        return pgApiLogRepository.findByOrderNoOrderByLoggedAtDesc(payment.getOrderNo()).stream()
                .map(PgLogResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalesResponse> getSales(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().minusDays(7) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return salesRepository.findByBusinessDateBetweenOrderByOccurredAtDesc(start, end).stream()
                .map(SalesResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addSalesAdjustment(Long salesId, SalesAdjustmentRequest request) {
        if (!StringUtils.hasText(request.adjustmentType()) || request.adjustmentAmount() == null) {
            throw new IllegalArgumentException("Adjustment type and amount are required.");
        }
        salesRepository.findById(salesId)
                .orElseThrow(() -> new IllegalArgumentException("Sales transaction not found."));
        adjustmentRepository.save(SettlementAdjustment.builder()
                .salesId(salesId)
                .adjustmentType(request.adjustmentType())
                .adjustmentAmount(request.adjustmentAmount())
                .reason(StringUtils.hasText(request.reason()) ? request.reason() : "Portfolio mock adjustment")
                .build());
        saveAudit("SALES", "ADJUSTMENT", String.valueOf(salesId), "매출 정산 조정 데이터를 등록했습니다.");
    }

    private void validateReadyRequest(InicisReadyRequest request) {
        if (request == null || !StringUtils.hasText(request.orderNo()) || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0 || !StringUtils.hasText(request.buyerName())
                || !StringUtils.hasText(request.productName())) {
            throw new IllegalArgumentException("Order number, positive amount, buyer name and product name are required.");
        }
    }

    private void validateAuthResultRequest(InicisAuthResultRequest request) {
        if (request == null || !StringUtils.hasText(request.mid()) || !StringUtils.hasText(request.orderNo())
                || !StringUtils.hasText(request.authToken()) || request.amount() == null
                || !StringUtils.hasText(request.resultCode()) || !StringUtils.hasText(request.signature())) {
            throw new IllegalArgumentException("Auth result request is invalid.");
        }
    }

    private void validateCancelRequest(PaymentCancelRequest request) {
        if (request == null || request.cancelAmount() == null || request.cancelAmount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(request.cancelRequestKey())) {
            throw new IllegalArgumentException("Cancel amount and cancel request key are required.");
        }
    }

    private void validateBridgeApproveRequest(PaymentApproveRequest request) {
        if (request == null || !StringUtils.hasText(request.orderNo()) || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order number and positive amount are required.");
        }
    }

    private void validateBridgeCancelRequest(PaymentBridgeCancelRequest request) {
        if (request == null || request.cancelAmount() == null || request.cancelAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Positive cancel amount is required.");
        }
    }

    private void validateAuthMatchesSession(InicisAuthResultRequest request, PaymentAuthSession session) {
        if (!session.getMid().equals(request.mid()) || !session.getOrderNo().equals(request.orderNo())
                || session.getAmount().compareTo(request.amount()) != 0) {
            throw new IllegalArgumentException("Auth result does not match payment session.");
        }
        if (!signatureService.matchesAuthSignature(request.orderNo(), request.amount(), request.authToken(), request.signature())) {
            throw new IllegalArgumentException("Auth signature is invalid.");
        }
    }

    private void validateApprovalResult(PaymentAuthSession session, PaymentGatewayAdapter.ApprovalResult approvalResult) {
        if (!StringUtils.hasText(approvalResult.tid()) || approvalResult.approvedAt() == null) {
            throw new IllegalArgumentException("Mock approval result is invalid.");
        }
        paymentRepository.findByTid(approvalResult.tid())
                .ifPresent(payment -> {
                    throw new IllegalArgumentException("Approved TID already exists.");
                });
        paymentRepository.findByOrderNo(session.getOrderNo())
                .ifPresent(payment -> {
                    throw new IllegalArgumentException("Order already has an approved payment.");
                });
    }

    private SalesTransaction createSales(PaymentTransaction payment, SaleType saleType, Long sourceId, BigDecimal amount, LocalDateTime occurredAt) {
        SalesTransaction sales = SalesTransaction.builder()
                .sourceType(saleType.name())
                .sourceId(sourceId)
                .orderNo(payment.getOrderNo())
                .tid(payment.getTid())
                .saleType(saleType)
                .saleAmount(amount)
                .vatAmount(amount.divide(BigDecimal.valueOf(11), 2, RoundingMode.HALF_UP))
                .saleStatus(SaleStatus.READY)
                .businessDate(occurredAt.toLocalDate())
                .occurredAt(occurredAt)
                .externalSendRequired(true)
                .settlementIncludedYn(false)
                .build();
        return salesRepository.save(sales);
    }

    private void createExternalSendRequest(SalesTransaction sales, String requestKey) {
        externalSendRequestRepository.save(ExternalSendRequest.builder()
                .salesId(sales.getId())
                .requestKey(requestKey)
                .targetSystem("SALES_OPERATION_MOCK")
                .sendStatus(ExternalSendStatus.READY)
                .retryCount(0)
                .build());
    }

    private PgApiLog savePgLog(Long paymentId, String orderNo, PgApiType apiType, String requestBody, LogResultStatus status, String message) {
        return pgApiLogRepository.save(PgApiLog.builder()
                .requestId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .orderNo(orderNo)
                .pgCompany(PG_COMPANY)
                .apiType(apiType)
                .requestBody(requestBody)
                .resultStatus(status)
                .resultMessage(message)
                .loggedAt(LocalDateTime.now())
                .build());
    }

    private PgApiLog savePgLog(
            Long paymentId,
            String orderNo,
            PgProvider pgProvider,
            PaymentEventType eventType,
            PgApiType apiType,
            String idempotencyKey,
            String requestBody,
            LogResultStatus status,
            String message) {
        return pgApiLogRepository.save(PgApiLog.builder()
                .requestId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .orderNo(orderNo)
                .pgCompany(PgCompany.valueOf(pgProvider.name().equals("MOCK") ? "INICIS" : pgProvider.name()))
                .pgProvider(pgProvider)
                .eventType(eventType)
                .apiType(apiType)
                .idempotencyKey(idempotencyKey)
                .requestBody(requestBody)
                .resultStatus(status)
                .resultMessage(message)
                .httpStatus(200)
                .successYn(LogResultStatus.SUCCESS.equals(status))
                .loggedAt(LocalDateTime.now())
                .build());
    }

    private void saveAudit(String domainType, String actionType, String referenceKey, String description) {
        auditLogRepository.save(AuditLog.builder()
                .domainType(domainType)
                .actionType(actionType)
                .referenceKey(referenceKey)
                .description(description)
                .loggedAt(LocalDateTime.now())
                .build());
    }

    private String defaultCurrency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim() : "KRW";
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String midByProvider(PgProvider provider) {
        if (PgProvider.INICIS.equals(provider)) {
            return inicisProperties.getMid();
        }
        return "MOCK_MID";
    }

    private PaymentTransaction saveUnknownPayment(PaymentApproveCommand command, PaymentApproveResult result) {
        PaymentTransaction payment = PaymentTransaction.builder()
                .mid(command.mid())
                .pgProvider(command.pgProvider())
                .orderNo(command.orderNo())
                .tid(result.tid() == null ? "UNKNOWN-" + UUID.randomUUID() : result.tid())
                .approvedAmount(command.amount())
                .canceledAmount(BigDecimal.ZERO)
                .currency(command.currency())
                .paymentStatus(PaymentStatus.UNKNOWN)
                .approvedAt(LocalDateTime.now())
                .failureReason(result.resultMessage())
                .build();
        return paymentRepository.save(payment);
    }

    private PaymentApproveResponse toApproveResponse(
            PaymentTransaction payment,
            PgProvider provider,
            String resultCode,
            String resultMessage) {
        return new PaymentApproveResponse(
                payment.getId(),
                provider,
                payment.getOrderNo(),
                payment.getTid(),
                payment.getPaymentStatus().name(),
                payment.getApprovedAmount(),
                payment.getApprovedAt(),
                resultCode,
                resultMessage
        );
    }

    public static String paymentMethod() {
        return PAYMENT_METHOD;
    }
}
