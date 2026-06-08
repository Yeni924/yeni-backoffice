package com.yeni.backoffice.api.payment.rest;

import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerLinksResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerPageResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesLedgerSummaryResponse;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.SalesResponse;
import com.yeni.backoffice.core.payment.service.SalesLedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/api/sales-ledger")
public class SalesLedgerRestController {

    private final SalesLedgerService salesLedgerService;

    public SalesLedgerRestController(SalesLedgerService salesLedgerService) {
        this.salesLedgerService = salesLedgerService;
    }

    @GetMapping
    public ResponseEntity<SalesLedgerPageResponse> ledger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String ledgerStatus,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(salesLedgerService.getSalesLedger(
                startDate, endDate, transactionType, ledgerStatus, settlementStatus, keyword, page, size));
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesLedgerSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String ledgerStatus,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(salesLedgerService.getSalesLedgerSummary(
                startDate, endDate, transactionType, ledgerStatus, settlementStatus, keyword));
    }

    @GetMapping("/{salesTransactionId}")
    public ResponseEntity<SalesResponse> detail(@PathVariable Long salesTransactionId) {
        return ResponseEntity.ok(salesLedgerService.getSalesLedgerDetail(salesTransactionId));
    }

    @GetMapping("/{salesTransactionId}/links")
    public ResponseEntity<SalesLedgerLinksResponse> links(@PathVariable Long salesTransactionId) {
        return ResponseEntity.ok(salesLedgerService.getSalesLedgerLinks(salesTransactionId));
    }
}
