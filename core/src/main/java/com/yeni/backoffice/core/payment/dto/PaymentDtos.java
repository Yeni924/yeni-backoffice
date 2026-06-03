package com.yeni.backoffice.core.payment.dto;

import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import com.yeni.backoffice.core.payment.entity.PaymentTransaction;
import com.yeni.backoffice.core.payment.entity.PgApiLog;
import com.yeni.backoffice.core.payment.entity.PgFeePolicy;
import com.yeni.backoffice.core.payment.entity.SalesTransaction;
import com.yeni.backoffice.core.payment.entity.SettlementDetail;
import com.yeni.backoffice.core.payment.entity.SettlementStatement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    @Schema(description = "Inicis StdPay 결제 준비 요청")
    public record InicisReadyRequest(
            @Schema(description = "내부 주문번호", example = "ORDER-20260603-001")
            String orderNo,
            @Schema(description = "결제 요청 금액", example = "12000")
            BigDecimal amount,
            @Schema(description = "통화 코드", example = "KRW")
            String currency,
            @Schema(description = "구매자명", example = "Portfolio Buyer")
            String buyerName,
            @Schema(description = "상품명", example = "PG Adapter Mock Item")
            String productName
    ) {
    }

    @Schema(description = "Inicis StdPay 결제 준비 응답")
    public record InicisReadyResponse(
            @Schema(description = "Mock 가맹점 ID")
            String mid,
            @Schema(description = "내부 주문번호")
            String orderNo,
            @Schema(description = "결제 요청 금액")
            BigDecimal amount,
            @Schema(description = "통화 코드")
            String currency,
            @Schema(description = "Mock 인증 토큰")
            String authToken,
            @Schema(description = "결제 요청 서명값")
            String signature,
            @Schema(description = "signKey 해시값. 실제 운영 키는 저장하지 않습니다.")
            String mKey,
            @Schema(description = "결제 결과 수신 URL")
            String returnUrl,
            @Schema(description = "결제창 닫기 URL")
            String closeUrl,
            @Schema(description = "표준결제 요청 파라미터")
            Map<String, String> stdPayParams
    ) {
    }

    @Schema(description = "Inicis StdPay 인증 결과 수신 요청")
    public record InicisAuthResultRequest(
            @Schema(description = "PG 가맹점 ID", example = "INIpayTest")
            String mid,
            @Schema(description = "내부 주문번호", example = "ORDER-20260603-001")
            String orderNo,
            @Schema(description = "결제 준비 단계에서 발급된 Mock 인증 토큰")
            String authToken,
            @Schema(description = "인증 금액", example = "12000")
            BigDecimal amount,
            @Schema(description = "PG 인증 결과 코드. 0000이면 성공", example = "0000")
            String resultCode,
            @Schema(description = "PG 인증 결과 메시지", example = "mock auth success")
            String resultMessage,
            @Schema(description = "주문번호, 금액, 인증 토큰 기반 검증 서명")
            String signature
    ) {
    }

    @Schema(description = "결제 승인 처리 응답")
    public record InicisApproveResponse(
            @Schema(description = "결제 거래 ID")
            Long paymentId,
            @Schema(description = "내부 주문번호")
            String orderNo,
            @Schema(description = "PG 거래번호")
            String tid,
            @Schema(description = "결제 상태", example = "APPROVED")
            String paymentStatus,
            @Schema(description = "승인 금액")
            BigDecimal approvedAmount,
            @Schema(description = "승인 시각")
            LocalDateTime approvedAt
    ) {
    }

    @Schema(description = "결제 취소 요청")
    public record PaymentCancelRequest(
            @Schema(description = "취소 금액. 승인 잔여 금액을 초과할 수 없습니다.", example = "3000")
            BigDecimal cancelAmount,
            @Schema(description = "취소 사유", example = "portfolio partial cancel")
            String cancelReason,
            @Schema(description = "취소 중복 처리 방지 키", example = "CANCEL-20260603-001")
            String cancelRequestKey
    ) {
    }

    @Schema(description = "결제 취소 응답")
    public record PaymentCancelResponse(
            @Schema(description = "취소 ID")
            Long cancelId,
            @Schema(description = "결제 거래 ID")
            Long paymentId,
            @Schema(description = "PG 거래번호")
            String tid,
            @Schema(description = "취소 금액")
            BigDecimal cancelAmount,
            @Schema(description = "취소 유형", example = "PARTIAL")
            String cancelType,
            @Schema(description = "취소 후 결제 상태", example = "PARTIAL_CANCELED")
            String paymentStatus
    ) {
    }

    @Schema(description = "결제 거래 조회 응답")
    public record PaymentResponse(
            @Schema(description = "결제 거래 ID")
            Long id,
            @Schema(description = "PG 가맹점 ID")
            String mid,
            @Schema(description = "내부 주문번호")
            String orderNo,
            @Schema(description = "PG 거래번호")
            String tid,
            @Schema(description = "승인 금액")
            BigDecimal approvedAmount,
            @Schema(description = "누적 취소 금액")
            BigDecimal canceledAmount,
            @Schema(description = "통화 코드")
            String currency,
            @Schema(description = "결제 상태")
            String paymentStatus,
            @Schema(description = "승인 시각")
            LocalDateTime approvedAt
    ) {
        public static PaymentResponse from(PaymentTransaction payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getMid(),
                    payment.getOrderNo(),
                    payment.getTid(),
                    payment.getApprovedAmount(),
                    payment.getCanceledAmount(),
                    payment.getCurrency(),
                    payment.getPaymentStatus().name(),
                    payment.getApprovedAt()
            );
        }
    }

    @Schema(description = "PG API 로그 조회 응답")
    public record PgLogResponse(
            Long id,
            String requestId,
            Long paymentId,
            String orderNo,
            String pgCompany,
            String apiType,
            String requestBody,
            String responseBody,
            String resultStatus,
            String resultMessage,
            LocalDateTime loggedAt
    ) {
        public static PgLogResponse from(PgApiLog log) {
            return new PgLogResponse(
                    log.getId(),
                    log.getRequestId(),
                    log.getPaymentId(),
                    log.getOrderNo(),
                    log.getPgCompany().name(),
                    log.getApiType().name(),
                    log.getRequestBody(),
                    log.getResponseBody(),
                    log.getResultStatus().name(),
                    log.getResultMessage(),
                    log.getLoggedAt()
            );
        }
    }

    @Schema(description = "매출 거래 조회 응답")
    public record SalesResponse(
            Long id,
            String orderNo,
            String tid,
            String saleType,
            BigDecimal saleAmount,
            BigDecimal vatAmount,
            String saleStatus,
            LocalDate businessDate,
            Boolean settlementIncludedYn
    ) {
        public static SalesResponse from(SalesTransaction sales) {
            return new SalesResponse(
                    sales.getId(),
                    sales.getOrderNo(),
                    sales.getTid(),
                    sales.getSaleType().name(),
                    sales.getSaleAmount(),
                    sales.getVatAmount(),
                    sales.getSaleStatus().name(),
                    sales.getBusinessDate(),
                    sales.getSettlementIncludedYn()
            );
        }
    }

    @Schema(description = "매출 정산 조정 요청")
    public record SalesAdjustmentRequest(
            @Schema(description = "조정 유형", example = "MANUAL_ADJUST")
            String adjustmentType,
            @Schema(description = "조정 금액", example = "1000")
            BigDecimal adjustmentAmount,
            @Schema(description = "조정 사유")
            String reason
    ) {
    }

    @Schema(description = "외부 전송 요청 조회 응답")
    public record ExternalSendResponse(
            Long id,
            Long salesId,
            String requestKey,
            String targetSystem,
            String sendStatus,
            Integer retryCount,
            String lastErrorMessage,
            LocalDateTime lastSentAt
    ) {
        public static ExternalSendResponse from(ExternalSendRequest request) {
            return new ExternalSendResponse(
                    request.getId(),
                    request.getSalesId(),
                    request.getRequestKey(),
                    request.getTargetSystem(),
                    request.getSendStatus().name(),
                    request.getRetryCount(),
                    request.getLastErrorMessage(),
                    request.getLastSentAt()
            );
        }
    }

    @Schema(description = "PG 수수료 정책 등록 요청")
    public record PgFeePolicyRequest(
            @Schema(description = "PG사", example = "INICIS")
            String pgCompany,
            @Schema(description = "가맹점 ID", example = "INIpayTest")
            String mid,
            @Schema(description = "결제수단", example = "CARD")
            String paymentMethod,
            @Schema(description = "수수료율", example = "0.0250")
            BigDecimal feeRate,
            @Schema(description = "적용 시작일")
            LocalDate effectiveStartDate,
            @Schema(description = "적용 종료일")
            LocalDate effectiveEndDate
    ) {
    }

    @Schema(description = "PG 수수료 정책 조회 응답")
    public record PgFeePolicyResponse(
            Long id,
            String pgCompany,
            String mid,
            String paymentMethod,
            BigDecimal feeRate,
            LocalDate effectiveStartDate,
            LocalDate effectiveEndDate,
            Boolean useYn
    ) {
        public static PgFeePolicyResponse from(PgFeePolicy policy) {
            return new PgFeePolicyResponse(
                    policy.getId(),
                    policy.getPgCompany(),
                    policy.getMid(),
                    policy.getPaymentMethod(),
                    policy.getFeeRate(),
                    policy.getEffectiveStartDate(),
                    policy.getEffectiveEndDate(),
                    policy.getUseYn()
            );
        }
    }

    @Schema(description = "정산 배치 실행 요청")
    public record SettlementBatchRunRequest(
            @Schema(description = "정산 대상 영업일")
            LocalDate targetDate
    ) {
    }

    @Schema(description = "정산 명세 조회 응답")
    public record SettlementStatementResponse(
            Long id,
            LocalDate settlementDate,
            String pgCompany,
            String mid,
            BigDecimal grossAmount,
            BigDecimal feeAmount,
            BigDecimal vatAmount,
            BigDecimal netAmount,
            String settlementStatus
    ) {
        public static SettlementStatementResponse from(SettlementStatement statement) {
            return new SettlementStatementResponse(
                    statement.getId(),
                    statement.getSettlementDate(),
                    statement.getPgCompany(),
                    statement.getMid(),
                    statement.getGrossAmount(),
                    statement.getFeeAmount(),
                    statement.getVatAmount(),
                    statement.getNetAmount(),
                    statement.getSettlementStatus().name()
            );
        }
    }

    @Schema(description = "정산 상세 조회 응답")
    public record SettlementDetailResponse(
            Long id,
            Long salesId,
            String saleType,
            BigDecimal saleAmount,
            BigDecimal feeAmount,
            BigDecimal netAmount
    ) {
        public static SettlementDetailResponse from(SettlementDetail detail) {
            return new SettlementDetailResponse(
                    detail.getId(),
                    detail.getSalesId(),
                    detail.getSaleType(),
                    detail.getSaleAmount(),
                    detail.getFeeAmount(),
                    detail.getNetAmount()
            );
        }
    }

    @Schema(description = "정산 명세 상세 화면 응답")
    public record SettlementDetailPageResponse(
            SettlementStatementResponse statement,
            List<SettlementDetailResponse> details
    ) {
    }

    @Schema(description = "PG 운영 요약 통계 응답")
    public record OperationSummaryResponse(
            LocalDate startDate,
            LocalDate endDate,
            long paymentCount,
            BigDecimal approvedAmount,
            long salesCount,
            BigDecimal salesAmount,
            long failedExternalSendCount
    ) {
    }
}
