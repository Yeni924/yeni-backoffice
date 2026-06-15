package com.yeni.backoffice.api.payment.rest;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.AlimtalkQueueResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.ExternalSendResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.OperationSummaryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgFeePolicyRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgFeePolicyResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesAdjustmentRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerRunRequest;
import com.yeni.backoffice.core.payment.dto.WorkerDtos.WorkerRunResult;
import com.yeni.backoffice.core.payment.repository.AlimtalkQueueRepository;
import com.yeni.backoffice.core.payment.repository.PaymentRecoveryTaskRepository;
import com.yeni.backoffice.core.payment.service.ExternalSendService;
import com.yeni.backoffice.core.payment.service.ExternalSendWorkerService;
import com.yeni.backoffice.core.payment.service.AlimtalkWorkerService;
import com.yeni.backoffice.core.payment.service.PaymentStatisticsService;
import com.yeni.backoffice.core.payment.service.PgFeePolicyService;
import com.yeni.backoffice.core.payment.service.SalesLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/admin", "/admin/api"})
@Tag(name = "Sales Operation", description = "매출 거래, 외부 전송, 수수료 정책, 운영 통계 API")
public class SalesOperationRestController {

    private final SalesLedgerService salesLedgerService;
    private final ExternalSendService externalSendService;
    private final PgFeePolicyService feePolicyService;
    private final PaymentStatisticsService statisticsService;
    private final AlimtalkQueueRepository alimtalkQueueRepository;
    private final PaymentRecoveryTaskRepository recoveryTaskRepository;
    private final ExternalSendWorkerService externalSendWorkerService;
    private final AlimtalkWorkerService alimtalkWorkerService;

    public SalesOperationRestController(
            SalesLedgerService salesLedgerService,
            ExternalSendService externalSendService,
            PgFeePolicyService feePolicyService,
            PaymentStatisticsService statisticsService,
            AlimtalkQueueRepository alimtalkQueueRepository,
            PaymentRecoveryTaskRepository recoveryTaskRepository,
            ExternalSendWorkerService externalSendWorkerService,
            AlimtalkWorkerService alimtalkWorkerService) {
        this.salesLedgerService = salesLedgerService;
        this.externalSendService = externalSendService;
        this.feePolicyService = feePolicyService;
        this.statisticsService = statisticsService;
        this.alimtalkQueueRepository = alimtalkQueueRepository;
        this.recoveryTaskRepository = recoveryTaskRepository;
        this.externalSendWorkerService = externalSendWorkerService;
        this.alimtalkWorkerService = alimtalkWorkerService;
    }

    @GetMapping("/sales")
    @Operation(summary = "매출 목록 조회", description = "영업일 기준 매출 거래 목록을 조회합니다.")
    public ResponseEntity<List<SalesResponse>> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(salesLedgerService.getSales(startDate, endDate));
    }

    @PostMapping("/sales/{salesId}/adjustments")
    @Operation(summary = "매출 정산 조정 등록", description = "매출 건에 대한 정산 조정 정보를 등록합니다.")
    public ResponseEntity<Void> addAdjustment(
            @PathVariable Long salesId,
            @Valid @RequestBody SalesAdjustmentRequest request) {
        salesLedgerService.addSalesAdjustment(salesId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/external-send-requests", "/external-send"})
    @Operation(summary = "외부 전송 요청 조회", description = "외부 영업관리 시스템 전송 요청 상태를 조회합니다.")
    public ResponseEntity<List<ExternalSendResponse>> externalSendRequests(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(externalSendService.getRequests(status));
    }

    @PostMapping({"/external-send-requests/{requestId}/send", "/external-send/{requestId}/send"})
    @Operation(summary = "외부 전송 실행", description = "외부 전송 요청 1건을 mock 전송 처리합니다.")
    public ResponseEntity<ExternalSendResponse> sendExternal(@PathVariable Long requestId) {
        return ResponseEntity.ok(externalSendService.send(requestId));
    }

    @PostMapping({"/external-send-requests/retry", "/external-send/retry"})
    @Operation(summary = "외부 전송 실패 재시도", description = "FAILED 상태의 외부 전송 요청을 재시도합니다.")
    public ResponseEntity<List<ExternalSendResponse>> retryExternalSend() {
        return ResponseEntity.ok(externalSendService.retryFailed());
    }

    @GetMapping({"/alimtalk-queues", "/alimtalk"})
    @Operation(summary = "알림톡 Queue 조회", description = "결제/취소 이벤트 후속 알림톡 발송 대기열을 조회합니다.")
    public ResponseEntity<List<AlimtalkQueueResponse>> alimtalkQueues() {
        return ResponseEntity.ok(alimtalkQueueRepository.findAll().stream()
                .map(AlimtalkQueueResponse::from)
                .toList());
    }

    @PostMapping("/external-send/worker/run")
    @Operation(summary = "외부전송 Worker 실행", description = "READY/FAILED 외부전송 요청을 Mock Worker로 처리합니다.")
    public ResponseEntity<WorkerRunResult> runExternalSendWorker(
            @RequestBody(required = false) WorkerRunRequest request) {
        int limit = request == null ? 20 : request.normalizedLimit();
        return ResponseEntity.ok(externalSendWorkerService.processReadyRequests(limit));
    }

    @PostMapping("/alimtalk/worker/run")
    @Operation(summary = "알림톡 Worker 실행", description = "READY/FAILED 알림톡 Queue를 Mock Worker로 처리합니다.")
    public ResponseEntity<WorkerRunResult> runAlimtalkWorker(
            @RequestBody(required = false) WorkerRunRequest request) {
        int limit = request == null ? 20 : request.normalizedLimit();
        return ResponseEntity.ok(alimtalkWorkerService.processReadyMessages(limit));
    }

    @GetMapping({"/payment-recovery-tasks", "/recovery"})
    @Operation(summary = "결제 Recovery Task 조회", description = "PG 결과불명, 망취소, 외부전송/알림톡 재처리 작업을 조회합니다.")
    public ResponseEntity<List<RecoveryTaskResponse>> recoveryTasks() {
        return ResponseEntity.ok(recoveryTaskRepository.findAll().stream()
                .map(RecoveryTaskResponse::from)
                .toList());
    }

    @GetMapping("/pg-fee-policies")
    @Operation(summary = "PG 수수료 정책 조회", description = "PG사, MID, 결제수단 기준 수수료 정책을 조회합니다.")
    public ResponseEntity<List<PgFeePolicyResponse>> feePolicies(
            @RequestParam(required = false) String pgCompany,
            @RequestParam(required = false) String mid,
            @RequestParam(required = false) String paymentMethod) {
        return ResponseEntity.ok(feePolicyService.getPolicies(pgCompany, mid, paymentMethod));
    }

    @PostMapping("/pg-fee-policies")
    @Operation(summary = "PG 수수료 정책 등록", description = "적용 기간 중복을 검증한 뒤 수수료 정책을 등록합니다.")
    public ResponseEntity<PgFeePolicyResponse> createFeePolicy(@Valid @RequestBody PgFeePolicyRequest request) {
        return ResponseEntity.ok(feePolicyService.createPolicy(request));
    }

    @GetMapping("/payment-statistics/summary")
    @Operation(summary = "PG 운영 요약 통계", description = "결제, 매출, 외부 전송 실패 건수를 요약 조회합니다.")
    public ResponseEntity<OperationSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.getSummary(startDate, endDate));
    }
}
