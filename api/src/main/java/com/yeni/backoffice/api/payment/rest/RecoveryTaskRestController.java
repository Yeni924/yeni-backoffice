package com.yeni.backoffice.api.payment.rest;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.RecoveryTaskResponse;
import com.yeni.backoffice.core.payment.service.PaymentRecoveryOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/api/recovery/tasks")
@Tag(name = "Recovery Task", description = "결제 복구 작업 운영 API")
public class RecoveryTaskRestController {

    private final PaymentRecoveryOperationService recoveryOperationService;

    public RecoveryTaskRestController(PaymentRecoveryOperationService recoveryOperationService) {
        this.recoveryOperationService = recoveryOperationService;
    }

    @GetMapping
    @Operation(summary = "복구 작업 목록 조회", description = "상태, 복구 유형, 키워드, 생성일 기준으로 RecoveryTask를 조회합니다.")
    public ResponseEntity<RecoveryTaskPageResponse> tasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recoveryType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(recoveryOperationService.getTasks(status, recoveryType, keyword, startDate, endDate, page, size));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "복구 작업 상세 조회", description = "RecoveryTask 단건의 추적 메타데이터와 처리 상태를 조회합니다.")
    public ResponseEntity<RecoveryTaskResponse> task(@PathVariable Long taskId) {
        return ResponseEntity.ok(recoveryOperationService.getTask(taskId));
    }

    @PostMapping("/{taskId}/retry")
    @Operation(summary = "복구 작업 재시도", description = "승인 결과불명 복구 작업은 PG 재조회와 후속 처리를 다시 수행합니다.")
    public ResponseEntity<RecoveryTaskResponse> retry(@PathVariable Long taskId) {
        return ResponseEntity.ok(recoveryOperationService.retry(taskId));
    }

    @PostMapping("/{taskId}/mark-success")
    @Operation(summary = "복구 작업 성공 처리", description = "운영자가 확인한 복구 작업을 성공 상태로 전이합니다.")
    public ResponseEntity<RecoveryTaskResponse> markSuccess(@PathVariable Long taskId) {
        return ResponseEntity.ok(recoveryOperationService.markSuccess(taskId));
    }

    @PostMapping("/{taskId}/mark-failed")
    @Operation(summary = "복구 작업 실패 처리", description = "운영자가 확인한 복구 작업을 실패 상태로 전이합니다.")
    public ResponseEntity<RecoveryTaskResponse> markFailed(@PathVariable Long taskId) {
        return ResponseEntity.ok(recoveryOperationService.markFailed(taskId));
    }
}
