package com.yeni.backoffice.api.payment.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment-bridge/callback")
@Tag(name = "Payment Callback", description = "PG사별 callback/webhook mock 수신 API")
public class PaymentCallbackRestController {

    @PostMapping("/{pgProvider}")
    @Operation(summary = "PG callback mock 수신", description = "PG사별 callback/webhook 구조 확장을 위한 mock endpoint입니다.")
    public ResponseEntity<Map<String, Object>> callback(
            @PathVariable String pgProvider,
            @RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of(
                "received", true,
                "pgProvider", pgProvider,
                "payload", payload == null ? Map.of() : payload
        ));
    }
}
