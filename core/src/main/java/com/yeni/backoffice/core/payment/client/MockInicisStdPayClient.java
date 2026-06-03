package com.yeni.backoffice.core.payment.client;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MockInicisStdPayClient {

    public MockApprovalResult approve(String mid, String orderNo, BigDecimal amount, String authToken) {
        if (orderNo != null && orderNo.toUpperCase().contains("FAIL")) {
            return new MockApprovalResult(false, null, "9999", "Mock approval failure.", null);
        }

        String tid = "INI-MOCK-" + UUID.nameUUIDFromBytes((mid + orderNo + authToken).getBytes());
        return new MockApprovalResult(true, tid, "0000", "Mock approval success.", LocalDateTime.now());
    }

    public MockCancelResult cancel(String tid, BigDecimal cancelAmount, String cancelRequestKey) {
        if (cancelRequestKey != null && cancelRequestKey.toUpperCase().contains("FAIL")) {
            return new MockCancelResult(false, "9999", "Mock cancel failure.");
        }
        return new MockCancelResult(true, "0000", "Mock cancel success.");
    }

    public void netCancel(String tid, String reason) {
        // Portfolio mock: 실제 PG 망 호출 없이 보상 트랜잭션 진입점을 코드로 표현한다.
    }

    public record MockApprovalResult(
            boolean success,
            String tid,
            String resultCode,
            String resultMessage,
            LocalDateTime approvedAt
    ) {
    }

    public record MockCancelResult(
            boolean success,
            String resultCode,
            String resultMessage
    ) {
    }
}
