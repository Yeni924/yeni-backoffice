package com.yeni.backoffice.api.payment.rest;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementBatchRunRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementDetailPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SettlementStatementResponse;
import com.yeni.backoffice.core.payment.service.SettlementOperationService;
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
@RequestMapping({"/api/admin/settlements", "/admin/api/settlements"})
@Tag(name = "Settlement Operation", description = "일별 정산 배치, 정산 명세 조회, 확정, 지급 처리 API")
public class SettlementOperationRestController {

    private final SettlementOperationService settlementOperationService;

    public SettlementOperationRestController(SettlementOperationService settlementOperationService) {
        this.settlementOperationService = settlementOperationService;
    }

    @PostMapping("/batch/run")
    @Operation(summary = "일별 정산 배치 실행", description = "미정산 매출을 기준으로 정산 초안을 생성합니다.")
    public ResponseEntity<SettlementStatementResponse> runBatch(@Valid @RequestBody(required = false) SettlementBatchRunRequest request) {
        return ResponseEntity.ok(settlementOperationService.runDailySettlement(request));
    }

    @GetMapping
    @Operation(summary = "정산 명세 목록 조회", description = "기간 기준 정산 명세 목록을 조회합니다.")
    public ResponseEntity<List<SettlementStatementResponse>> statements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(settlementOperationService.getStatements(startDate, endDate));
    }

    @GetMapping("/{statementId}")
    @Operation(summary = "정산 명세 상세 조회", description = "정산 명세와 포함된 매출 상세를 조회합니다.")
    public ResponseEntity<SettlementDetailPageResponse> statement(@PathVariable Long statementId) {
        return ResponseEntity.ok(settlementOperationService.getStatement(statementId));
    }

    @PostMapping("/{statementId}/confirm")
    @Operation(summary = "정산 확정", description = "DRAFT 상태의 정산 명세를 CONFIRMED 상태로 변경합니다.")
    public ResponseEntity<SettlementStatementResponse> confirm(@PathVariable Long statementId) {
        return ResponseEntity.ok(settlementOperationService.confirmStatement(statementId));
    }

    @PostMapping("/{statementId}/pay")
    @Operation(summary = "정산 지급 처리", description = "CONFIRMED 상태의 정산 명세를 PAID 상태로 변경합니다.")
    public ResponseEntity<SettlementStatementResponse> pay(@PathVariable Long statementId) {
        return ResponseEntity.ok(settlementOperationService.markPaid(statementId));
    }
}
