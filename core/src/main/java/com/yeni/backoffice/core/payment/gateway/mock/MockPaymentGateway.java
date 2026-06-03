package com.yeni.backoffice.core.payment.gateway.mock;

import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.gateway.PaymentGateway;
import com.yeni.backoffice.core.payment.gateway.command.PaymentApproveCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentCancelCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentQueryCommand;
import com.yeni.backoffice.core.payment.gateway.result.PaymentApproveResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentCancelResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentQueryResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PgProvider provider() {
        return PgProvider.MOCK;
    }

    @Override
    public PaymentApproveResult approve(PaymentApproveCommand command) {
        if (command.orderNo().toUpperCase().contains("UNKNOWN")) {
            return new PaymentApproveResult(provider(), false, PaymentStatus.UNKNOWN, null, "U000", "Mock approve result unknown.", null, true);
        }
        if (command.orderNo().toUpperCase().contains("FAIL")) {
            return new PaymentApproveResult(provider(), false, PaymentStatus.APPROVE_FAILED, null, "9999", "Mock approve failure.", null, false);
        }
        String tid = "MOCK-" + UUID.nameUUIDFromBytes((command.orderNo() + command.idempotencyKey()).getBytes());
        return new PaymentApproveResult(provider(), true, PaymentStatus.APPROVED, tid, "0000", "Mock approve success.", LocalDateTime.now(), false);
    }

    @Override
    public PaymentCancelResult cancel(PaymentCancelCommand command) {
        if (command.idempotencyKey().toUpperCase().contains("UNKNOWN")) {
            return new PaymentCancelResult(provider(), false, PaymentStatus.UNKNOWN, "U000", "Mock cancel result unknown.", true);
        }
        if (command.idempotencyKey().toUpperCase().contains("FAIL")) {
            return new PaymentCancelResult(provider(), false, PaymentStatus.CANCEL_FAILED, "9999", "Mock cancel failure.", false);
        }
        return new PaymentCancelResult(provider(), true, PaymentStatus.CANCELED, "0000", "Mock cancel success.", false);
    }

    @Override
    public PaymentQueryResult query(PaymentQueryCommand command) {
        if (command.orderNo() != null && command.orderNo().toUpperCase().contains("UNKNOWN")) {
            return new PaymentQueryResult(provider(), false, PaymentStatus.UNKNOWN, command.tid(), "U000", "Mock query still unknown.");
        }
        return new PaymentQueryResult(provider(), true, PaymentStatus.APPROVED, command.tid(), "0000", "Mock query confirmed approved.");
    }
}
