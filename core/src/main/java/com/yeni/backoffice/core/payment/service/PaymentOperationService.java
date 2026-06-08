package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.BusinessException;
import com.yeni.backoffice.core.common.exception.ConflictException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.NotFoundException;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
import com.yeni.backoffice.core.payment.adapter.PaymentGatewayAdapter;
import com.yeni.backoffice.core.payment.adapter.PaymentGatewayAdapterResolver;
import com.yeni.backoffice.core.payment.config.InicisStdPayProperties;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisAuthResultRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisReadyRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.InicisReadyResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.AlimtalkQueueResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ExternalSendResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgLogResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesAdjustmentRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerLinksResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerSummaryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentQueryResponse;
import com.yeni.backoffice.core.payment.entity.AuditLog;
import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentAuthSession;
import com.yeni.backoffice.core.payment.entity.PaymentCancel;
import com.yeni.backoffice.core.payment.entity.PaymentRecoveryTask;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.PgApiLog;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementAdjustment;
import com.yeni.backoffice.core.payment.enums.AlimtalkStatus;
import com.yeni.backoffice.core.payment.enums.CancelStatus;
import com.yeni.backoffice.core.payment.enums.CancelType;
import com.yeni.backoffice.core.payment.enums.ExternalSendStatus;
import com.yeni.backoffice.core.payment.enums.LedgerStatus;
import com.yeni.backoffice.core.payment.enums.LogResultStatus;
import com.yeni.backoffice.core.payment.enums.PaymentAuthStatus;
import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PaymentEventType;
import com.yeni.backoffice.core.payment.enums.PgApiType;
import com.yeni.backoffice.core.payment.enums.PgCompany;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.enums.RecoveryStatus;
import com.yeni.backoffice.core.payment.enums.RecoveryType;
import com.yeni.backoffice.core.payment.enums.SaleStatus;
import com.yeni.backoffice.core.payment.enums.SaleType;
import com.yeni.backoffice.core.payment.enums.SalesSettlementStatus;
import com.yeni.backoffice.core.payment.repository.AuditLogRepository;
import com.yeni.backoffice.core.payment.repository.PaymentAuthSessionRepository;
import com.yeni.backoffice.core.payment.repository.PaymentCancelRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.repository.PaymentTransactionRepository;
import com.yeni.backoffice.core.payment.repository.PgApiLogRepository;
import com.yeni.backoffice.core.payment.repository.SalesTransactionRepository;
import com.yeni.backoffice.core.payment.repository.SettlementAdjustmentRepository;
import com.yeni.backoffice.core.payment.support.PaymentDefaults;
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

    private final InicisStdPayProperties inicisProperties;
    private final InicisSignatureService signatureService;
    private final PaymentGatewayAdapterResolver adapterResolver;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentGatewayRouter gatewayRouter;
    private final PaymentAuthSessionRepository authSessionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final PaymentCancelRepository cancelRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final PgApiLogRepository pgApiLogRepository;
    private final SalesTransactionRepository salesRepository;
    private final SettlementAdjustmentRepository adjustmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final SalesLedgerService salesLedgerService;
    private final PaymentFollowUpService followUpService;

    public PaymentOperationService(
            InicisStdPayProperties inicisProperties,
            InicisSignatureService signatureService,
            PaymentGatewayAdapterResolver adapterResolver,
            PaymentGatewayRegistry gatewayRegistry,
            PaymentGatewayRouter gatewayRouter,
            PaymentAuthSessionRepository authSessionRepository,
            PaymentTransactionRepository paymentRepository,
            PaymentCancelRepository cancelRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            PgApiLogRepository pgApiLogRepository,
            SalesTransactionRepository salesRepository,
            SettlementAdjustmentRepository adjustmentRepository,
            AuditLogRepository auditLogRepository,
            SalesLedgerService salesLedgerService,
            PaymentFollowUpService followUpService) {
        this.inicisProperties = inicisProperties;
        this.signatureService = signatureService;
        this.adapterResolver = adapterResolver;
        this.gatewayRegistry = gatewayRegistry;
        this.gatewayRouter = gatewayRouter;
        this.authSessionRepository = authSessionRepository;
        this.paymentRepository = paymentRepository;
        this.cancelRepository = cancelRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.pgApiLogRepository = pgApiLogRepository;
        this.salesRepository = salesRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.salesLedgerService = salesLedgerService;
        this.followUpService = followUpService;
    }

    @Transactional
    public PaymentApproveResponse approvePayment(PaymentApproveRequest request) {
        validateBridgeApproveRequest(request);
        String idempotencyKey = defaultText(request.idempotencyKey(), "APPROVE-" + request.orderNo());
        PaymentTransaction existingPayment = paymentRepository.findByApprovalRequestKey(idempotencyKey)
                .or(() -> paymentRepository.findByOrderNo(request.orderNo()))
                .orElse(null);
        if (existingPayment != null) {
            return toApproveResponse(existingPayment, existingPayment.getPgProvider(), "IDEMPOTENT_REPLAY", "Existing approve result returned.");
        }

        PgProvider provider = gatewayRouter.route(
                request.pgProvider(),
                request.channelType(),
                request.storeCode(),
                defaultText(request.paymentMethod(), PaymentDefaults.PAYMENT_METHOD_CARD)
        );
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
                defaultText(request.paymentMethod(), PaymentDefaults.PAYMENT_METHOD_CARD)
        );

        long startedAt = System.currentTimeMillis();
        PgApiLog apiLog = savePgLog(null, request.orderNo(), provider, PaymentEventType.APPROVE, PgApiType.APPROVE,
                idempotencyKey, command.toString(), LogResultStatus.REQUESTED, "PGB approve requested");
        PaymentApproveResult result = gateway.approve(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage(), result.tid());

        if (result.unknown()) {
            PaymentTransaction payment = saveUnknownPayment(command, result);
            followUpService.createRecoveryTask(payment.getId(), null, payment.getOrderNo(), payment.getTid(), idempotencyKey,
                    RecoveryType.APPROVE_UNKNOWN_CHECK, "APPROVE_UNKNOWN-" + payment.getOrderNo(), result.resultMessage());
            saveAudit("PAYMENT", "UNKNOWN", request.orderNo(), "PGB approve result is unknown. retry-query is required.");
            return toApproveResponse(payment, provider, result.resultCode(), result.resultMessage());
        }
        if (!result.success()) {
            saveAudit("PAYMENT", "APPROVE_FAILED", request.orderNo(), result.resultMessage());
            throw new BusinessException(ErrorCode.PAYMENT_APPROVE_FAILED, "결제 승인에 실패했습니다: " + result.resultMessage());
        }

        PaymentTransaction payment = PaymentTransaction.builder()
                .mid(command.mid())
                .pgProvider(provider)
                .orderNo(command.orderNo())
                .tid(result.tid())
                .approvalRequestKey(idempotencyKey)
                .approvedAmount(command.amount())
                .canceledAmount(BigDecimal.ZERO)
                .currency(command.currency())
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(result.approvedAt())
                .build();
        try {
            paymentRepository.save(payment);
            SalesTransaction sales = salesLedgerService.createSales(payment, SaleType.SALE, payment.getId(), payment.getApprovedAmount(), result.approvedAt());
            followUpService.createExternalSendRequest(sales, "SALE-" + payment.getOrderNo());
            followUpService.createAlimtalkQueue(payment, sales, "SALE-" + payment.getOrderNo(), "APPROVE");
            saveAudit("PAYMENT", "APPROVED", payment.getOrderNo(), "PGB approve completed through " + provider);
            return toApproveResponse(payment, provider, result.resultCode(), result.resultMessage() + " (" + (System.currentTimeMillis() - startedAt) + "ms)");
        } catch (RuntimeException internalFailure) {
            RecoveryType recoveryType = payment.getId() == null
                    ? RecoveryType.APPROVE_INTERNAL_SAVE_FAILED
                    : RecoveryType.NETWORK_CANCEL;
            String taskKey = recoveryType.name() + "-" + command.orderNo();
            if (payment.getId() != null) {
                payment.updateStatus(PaymentStatus.NETWORK_CANCEL_REQUIRED, internalFailure.getMessage());
            }
            followUpService.createRecoveryTask(payment.getId(), null, command.orderNo(), result.tid(), idempotencyKey,
                    recoveryType, taskKey, internalFailure.getMessage());
            saveAudit("PAYMENT", "APPROVE_INTERNAL_FAILED", request.orderNo(),
                    "PG approve succeeded but internal processing failed: " + internalFailure.getMessage());
            return toApproveResponse(payment, provider, "NETWORK_CANCEL_REQUIRED",
                    "PG 승인 후 내부 처리 실패로 망취소/복구 대상에 등록했습니다.");
        }
    }

    @Transactional
    public PaymentBridgeCancelResponse cancelPaymentBridge(Long paymentId, PaymentBridgeCancelRequest request) {
        validateBridgeCancelRequest(request);
        PaymentTransaction payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        String idempotencyKey = request.idempotencyKey().trim();
        PaymentCancel existingCancel = cancelRepository.findByCancelRequestKey(idempotencyKey).orElse(null);
        if (existingCancel != null) {
            return new PaymentBridgeCancelResponse(
                    existingCancel.getId(),
                    payment.getId(),
                    payment.getPgProvider(),
                    payment.getTid(),
                    existingCancel.getCancelAmount(),
                    existingCancel.getCancelType().name(),
                    payment.getPaymentStatus().name(),
                    "IDEMPOTENT_REPLAY",
                    "Existing cancel result returned."
            );
        }
        String cancelUnknownTaskKey = "CANCEL_UNKNOWN-" + idempotencyKey;
        if (recoveryTaskRepository.findByTaskKey(cancelUnknownTaskKey).isPresent()) {
            return new PaymentBridgeCancelResponse(null, payment.getId(), payment.getPgProvider(), payment.getTid(),
                    request.cancelAmount(), null, payment.getPaymentStatus().name(), "IDEMPOTENT_REPLAY",
                    "Existing unknown cancel recovery task returned.");
        }
        if (payment.isCancelCompleted()) {
            throw new ConflictException(ErrorCode.PAYMENT_ALREADY_CANCELED);
        }
        if (request.cancelAmount().compareTo(payment.getCancelableAmount()) > 0) {
            throw new ValidationBusinessException(ErrorCode.PAYMENT_CANCEL_AMOUNT_EXCEEDED);
        }

        PgProvider provider = request.pgProvider() == null ? payment.getPgProvider() : request.pgProvider();
        PaymentGateway gateway = gatewayRegistry.get(provider);
        PaymentCancelCommand command = new PaymentCancelCommand(provider, payment.getTid(), request.cancelAmount(), idempotencyKey, request.cancelReason());
        PgApiLog apiLog = savePgLog(payment.getId(), payment.getOrderNo(), provider, PaymentEventType.CANCEL, PgApiType.CANCEL,
                idempotencyKey, command.toString(), LogResultStatus.REQUESTED, "PGB cancel requested");
        PaymentCancelResult result = gateway.cancel(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage());

        if (result.unknown()) {
            payment.updateStatus(PaymentStatus.CANCEL_UNKNOWN, result.resultMessage());
            followUpService.createRecoveryTask(payment.getId(), null, payment.getOrderNo(), payment.getTid(), idempotencyKey,
                    RecoveryType.CANCEL_UNKNOWN_CHECK, cancelUnknownTaskKey, result.resultMessage());
            saveAudit("PAYMENT", "CANCEL_UNKNOWN", payment.getOrderNo(), result.resultMessage());
            return new PaymentBridgeCancelResponse(null, payment.getId(), provider, payment.getTid(), request.cancelAmount(),
                    null, payment.getPaymentStatus().name(), result.resultCode(), result.resultMessage());
        }
        if (!result.success()) {
            payment.updateStatus(PaymentStatus.CANCEL_FAILED, result.resultMessage());
            saveAudit("PAYMENT", "CANCEL_FAILED", payment.getOrderNo(), result.resultMessage());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED, "결제 취소에 실패했습니다: " + result.resultMessage());
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
        SalesTransaction sales = salesLedgerService.createSales(payment, SaleType.CANCEL, cancel.getId(), request.cancelAmount().negate(), cancel.getCanceledAt());
        followUpService.createExternalSendRequest(sales, "CANCEL-" + idempotencyKey);
        followUpService.createAlimtalkQueue(payment, sales, "CANCEL-" + idempotencyKey, "CANCEL");
        saveAudit("PAYMENT", "CANCELED", payment.getOrderNo(), "PGB cancel completed through " + provider);

        return new PaymentBridgeCancelResponse(cancel.getId(), payment.getId(), provider, payment.getTid(), cancel.getCancelAmount(),
                cancel.getCancelType().name(), payment.getPaymentStatus().name(), result.resultCode(), result.resultMessage());
    }

    @Transactional
    public PaymentQueryResponse retryQuery(Long paymentId) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        PaymentGateway gateway = gatewayRegistry.get(payment.getPgProvider());
        PaymentQueryCommand command = new PaymentQueryCommand(payment.getPgProvider(), payment.getTid(), payment.getOrderNo());
        PgApiLog apiLog = savePgLog(payment.getId(), payment.getOrderNo(), payment.getPgProvider(), PaymentEventType.QUERY,
                PgApiType.INQUIRY, null, command.toString(), LogResultStatus.REQUESTED, "PGB query requested");
        PaymentQueryResult result = gateway.query(command);
        apiLog.complete(result.toString(), result.success() ? LogResultStatus.SUCCESS : LogResultStatus.FAILED, result.resultMessage());
        if (result.success() && (PaymentStatus.UNKNOWN.equals(payment.getPaymentStatus())
                || PaymentStatus.APPROVE_UNKNOWN.equals(payment.getPaymentStatus()))) {
            payment.updateStatus(result.paymentStatus(), null);
            if (PaymentStatus.APPROVED.equals(result.paymentStatus())) {
                SalesTransaction sales = salesLedgerService.createSales(payment, SaleType.SALE, payment.getId(), payment.getApprovedAmount(), payment.getApprovedAt());
                followUpService.createExternalSendRequest(sales, "SALE-" + payment.getOrderNo());
                followUpService.createAlimtalkQueue(payment, sales, "SALE-" + payment.getOrderNo(), "APPROVE");
                followUpService.markRecoverySuccess("APPROVE_UNKNOWN-" + payment.getOrderNo());
                saveAudit("PAYMENT", "APPROVE_UNKNOWN_RECOVERED", payment.getOrderNo(),
                        "PG query confirmed approve success and created missing sales/follow-up records.");
            }
        } else if (!result.success() && (PaymentStatus.UNKNOWN.equals(payment.getPaymentStatus())
                || PaymentStatus.APPROVE_UNKNOWN.equals(payment.getPaymentStatus()))) {
            payment.updateStatus(PaymentStatus.APPROVE_FAILED, result.resultMessage());
            recoveryTaskRepository.findByTaskKey("APPROVE_UNKNOWN-" + payment.getOrderNo())
                    .ifPresent(task -> task.markFailed(result.resultMessage()));
            saveAudit("PAYMENT", "APPROVE_UNKNOWN_FAILED", payment.getOrderNo(),
                    "PG query confirmed approve failure. SALE ledger was not created.");
        }
        return PaymentQueryResponse.from(payment, payment.getPgProvider(), result.resultCode(), result.resultMessage());
    }

    @Transactional
    public InicisReadyResponse prepareStdPay(InicisReadyRequest request) {
        validateReadyRequest(request);
        authSessionRepository.findByOrderNo(request.orderNo())
                .ifPresent(session -> {
                    throw new ConflictException(ErrorCode.PAYMENT_DUPLICATED_REQUEST, "이미 결제 세션이 생성된 주문번호입니다.");
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "결제 인증 세션을 찾을 수 없습니다."));
        session.markAuthResultReceived();
        savePgLog(null, request.orderNo(), PgApiType.AUTH_RESULT, request.toString(), LogResultStatus.RECEIVED, request.resultMessage());

        if (session.isExpired(LocalDateTime.now())) {
            session.markFailed();
            throw new ValidationBusinessException(ErrorCode.INVALID_REQUEST, "결제 인증 세션이 만료되었습니다.");
        }
        if (!"0000".equals(request.resultCode())) {
            session.markFailed();
            throw new BusinessException(ErrorCode.PAYMENT_APPROVE_FAILED, "PG 인증 결과가 실패입니다: " + request.resultMessage());
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
            throw new BusinessException(ErrorCode.PAYMENT_APPROVE_FAILED, "PG 승인에 실패했습니다: " + approvalResult.resultMessage());
        }

        try {
            validateApprovalResult(session, approvalResult);
            PaymentTransaction payment = PaymentTransaction.builder()
                    .mid(session.getMid())
                    .orderNo(session.getOrderNo())
                    .tid(approvalResult.tid())
                    .approvalRequestKey("STD_PAY-" + session.getOrderNo())
                    .approvedAmount(session.getAmount())
                    .canceledAmount(BigDecimal.ZERO)
                    .currency(session.getCurrency())
                    .paymentStatus(PaymentStatus.APPROVED)
                    .approvedAt(approvalResult.approvedAt())
                    .build();
            paymentRepository.save(payment);
            session.markApproved(approvalResult.tid());

            SalesTransaction sales = salesLedgerService.createSales(payment, SaleType.SALE, payment.getId(), payment.getApprovedAmount(), approvalResult.approvedAt());
            followUpService.createExternalSendRequest(sales, "SALE-" + payment.getOrderNo());
            followUpService.createAlimtalkQueue(payment, sales, "SALE-" + payment.getOrderNo(), "APPROVE");

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
        PaymentTransaction payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
        PaymentCancel existingCancel = cancelRepository.findByCancelRequestKey(request.cancelRequestKey()).orElse(null);
        if (existingCancel != null) {
            return new PaymentCancelResponse(
                    existingCancel.getId(),
                    payment.getId(),
                    payment.getTid(),
                    existingCancel.getCancelAmount(),
                    existingCancel.getCancelType().name(),
                    payment.getPaymentStatus().name()
            );
        }

        if (payment.isCancelCompleted()) {
            throw new ConflictException(ErrorCode.PAYMENT_ALREADY_CANCELED);
        }
        if (request.cancelAmount().compareTo(payment.getCancelableAmount()) > 0) {
            throw new ValidationBusinessException(ErrorCode.PAYMENT_CANCEL_AMOUNT_EXCEEDED);
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
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED, "PG 취소에 실패했습니다: " + result.resultMessage());
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

        SalesTransaction sales = salesLedgerService.createSales(payment, SaleType.CANCEL, cancel.getId(), request.cancelAmount().negate(), cancel.getCanceledAt());
        followUpService.createExternalSendRequest(sales, "CANCEL-" + request.cancelRequestKey());
        followUpService.createAlimtalkQueue(payment, sales, "CANCEL-" + request.cancelRequestKey(), "CANCEL");
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
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<PgLogResponse> getPaymentLogs(Long paymentId) {
        PaymentTransaction payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND));
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

    @Transactional(readOnly = true)
    public SalesLedgerPageResponse getSalesLedger(
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String ledgerStatus,
            String settlementStatus,
            String keyword,
            int page,
            int size) {
        return salesLedgerService.getSalesLedger(startDate, endDate, transactionType, ledgerStatus, settlementStatus, keyword, page, size);
    }

    @Transactional(readOnly = true)
    public SalesLedgerSummaryResponse getSalesLedgerSummary(
            LocalDate startDate,
            LocalDate endDate,
            String transactionType,
            String ledgerStatus,
            String settlementStatus,
            String keyword) {
        return salesLedgerService.getSalesLedgerSummary(startDate, endDate, transactionType, ledgerStatus, settlementStatus, keyword);
    }

    @Transactional(readOnly = true)
    public SalesResponse getSalesLedgerDetail(Long salesTransactionId) {
        return salesLedgerService.getSalesLedgerDetail(salesTransactionId);
    }

    @Transactional(readOnly = true)
    public SalesLedgerLinksResponse getSalesLedgerLinks(Long salesTransactionId) {
        return salesLedgerService.getSalesLedgerLinks(salesTransactionId);
    }

    @Transactional
    public void addSalesAdjustment(Long salesId, SalesAdjustmentRequest request) {
        if (!StringUtils.hasText(request.adjustmentType()) || request.adjustmentAmount() == null) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "조정 유형과 조정금액은 필수입니다.");
        }
        salesRepository.findById(salesId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALES_TRANSACTION_NOT_FOUND));
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
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "주문번호, 0보다 큰 금액, 구매자명, 상품명은 필수입니다.");
        }
    }

    private void validateAuthResultRequest(InicisAuthResultRequest request) {
        if (request == null || !StringUtils.hasText(request.mid()) || !StringUtils.hasText(request.orderNo())
                || !StringUtils.hasText(request.authToken()) || request.amount() == null
                || !StringUtils.hasText(request.resultCode()) || !StringUtils.hasText(request.signature())) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "인증 결과 요청값이 올바르지 않습니다.");
        }
    }

    private void validateCancelRequest(PaymentCancelRequest request) {
        if (request == null || request.cancelAmount() == null || request.cancelAmount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(request.cancelRequestKey())) {
            throw new ValidationBusinessException(ErrorCode.PAYMENT_CANCEL_KEY_REQUIRED);
        }
    }

    private void validateBridgeApproveRequest(PaymentApproveRequest request) {
        if (request == null || !StringUtils.hasText(request.orderNo()) || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationBusinessException(ErrorCode.VALIDATION_ERROR, "주문번호와 0보다 큰 승인금액은 필수입니다.");
        }
    }

    private void validateBridgeCancelRequest(PaymentBridgeCancelRequest request) {
        if (request == null || request.cancelAmount() == null || request.cancelAmount().compareTo(BigDecimal.ZERO) <= 0
                || !StringUtils.hasText(request.idempotencyKey())) {
            throw new ValidationBusinessException(ErrorCode.PAYMENT_CANCEL_KEY_REQUIRED, "0보다 큰 취소금액과 취소 중복 방지 키는 필수입니다.");
        }
    }

    private void validateAuthMatchesSession(InicisAuthResultRequest request, PaymentAuthSession session) {
        if (!session.getMid().equals(request.mid()) || !session.getOrderNo().equals(request.orderNo())
                || session.getAmount().compareTo(request.amount()) != 0) {
            throw new ValidationBusinessException(ErrorCode.INVALID_REQUEST, "인증 결과가 결제 세션과 일치하지 않습니다.");
        }
        if (!signatureService.matchesAuthSignature(request.orderNo(), request.amount(), request.authToken(), request.signature())) {
            throw new ValidationBusinessException(ErrorCode.INVALID_REQUEST, "인증 서명이 올바르지 않습니다.");
        }
    }

    private void validateApprovalResult(PaymentAuthSession session, PaymentGatewayAdapter.ApprovalResult approvalResult) {
        if (!StringUtils.hasText(approvalResult.tid()) || approvalResult.approvedAt() == null) {
            throw new ValidationBusinessException(ErrorCode.INVALID_REQUEST, "Mock 승인 응답값이 올바르지 않습니다.");
        }
        paymentRepository.findByTid(approvalResult.tid())
                .ifPresent(payment -> {
                    throw new ConflictException(ErrorCode.PAYMENT_DUPLICATED_REQUEST, "이미 등록된 PG 거래번호입니다.");
                });
        paymentRepository.findByOrderNo(session.getOrderNo())
                .ifPresent(payment -> {
                    throw new ConflictException(ErrorCode.PAYMENT_ALREADY_APPROVED);
                });
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
                .approvalRequestKey(command.idempotencyKey())
                .approvedAmount(command.amount())
                .canceledAmount(BigDecimal.ZERO)
                .currency(command.currency())
                .paymentStatus(PaymentStatus.APPROVE_UNKNOWN)
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
        return PaymentDefaults.PAYMENT_METHOD_CARD;
    }
}
