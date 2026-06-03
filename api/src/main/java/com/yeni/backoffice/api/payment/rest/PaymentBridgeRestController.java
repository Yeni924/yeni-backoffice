package com.yeni.backoffice.api.payment.rest;

import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentApproveResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelRequest;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentBridgeCancelResponse;
import com.yeni.backoffice.core.payment.dto.PaymentBridgeDtos.PaymentQueryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PaymentResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgLogResponse;
import com.yeni.backoffice.core.payment.service.PaymentOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment-bridge")
@Tag(name = "Payment Bridge", description = "PGB 공통 결제 승인/취소/재조회 API")
public class PaymentBridgeRestController {

    private final PaymentOperationService paymentOperationService;

    public PaymentBridgeRestController(PaymentOperationService paymentOperationService) {
        this.paymentOperationService = paymentOperationService;
    }

    @PostMapping("/payments/approve")
    @Operation(summary = "공통 결제 승인", description = "요청의 pgProvider를 기준으로 Gateway를 라우팅하고 결제 승인 mock을 처리합니다.")
    public ResponseEntity<PaymentApproveResponse> approve(@RequestBody PaymentApproveRequest request) {
        return ResponseEntity.ok(paymentOperationService.approvePayment(request));
    }

    @PostMapping("/payments/{paymentId}/cancel")
    @Operation(summary = "공통 결제 취소", description = "결제 거래 기준으로 전체/부분 취소를 처리하고 취소 매출을 생성합니다.")
    public ResponseEntity<PaymentBridgeCancelResponse> cancel(
            @PathVariable Long paymentId,
            @RequestBody PaymentBridgeCancelRequest request) {
        return ResponseEntity.ok(paymentOperationService.cancelPaymentBridge(paymentId, request));
    }

    @PostMapping("/payments/{paymentId}/retry-query")
    @Operation(summary = "UNKNOWN 거래 재조회", description = "승인/취소 결과가 UNKNOWN인 거래를 PG 거래조회 mock으로 재확인합니다.")
    public ResponseEntity<PaymentQueryResponse> retryQuery(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentOperationService.retryQuery(paymentId));
    }

    @GetMapping("/payments")
    @Operation(summary = "결제 목록 조회", description = "PGB에서 처리한 결제 거래 목록을 조회합니다.")
    public ResponseEntity<List<PaymentResponse>> payments() {
        return ResponseEntity.ok(paymentOperationService.getPayments());
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(summary = "결제 상세 조회", description = "결제 거래 ID 기준으로 결제 상세를 조회합니다.")
    public ResponseEntity<PaymentResponse> payment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentOperationService.getPayment(paymentId));
    }

    @GetMapping("/payments/{paymentId}/pg-logs")
    @Operation(summary = "결제 PG 로그 조회", description = "결제 거래의 요청/응답/재조회 로그를 조회합니다.")
    public ResponseEntity<List<PgLogResponse>> paymentLogs(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentOperationService.getPaymentLogs(paymentId));
    }
}
