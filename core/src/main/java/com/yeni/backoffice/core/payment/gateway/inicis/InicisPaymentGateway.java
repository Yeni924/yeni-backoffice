package com.yeni.backoffice.core.payment.gateway.inicis;

import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.gateway.PaymentGateway;
import com.yeni.backoffice.core.payment.gateway.command.PaymentApproveCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentCancelCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentQueryCommand;
import com.yeni.backoffice.core.payment.gateway.mock.MockPaymentGateway;
import com.yeni.backoffice.core.payment.gateway.result.PaymentApproveResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentCancelResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentQueryResult;
import org.springframework.stereotype.Component;

@Component
public class InicisPaymentGateway implements PaymentGateway {

    private final MockPaymentGateway mockPaymentGateway;

    public InicisPaymentGateway(MockPaymentGateway mockPaymentGateway) {
        this.mockPaymentGateway = mockPaymentGateway;
    }

    @Override
    public PgProvider provider() {
        return PgProvider.INICIS;
    }

    @Override
    public PaymentApproveResult approve(PaymentApproveCommand command) {
        PaymentApproveResult result = mockPaymentGateway.approve(command);
        return new PaymentApproveResult(provider(), result.success(), result.paymentStatus(), result.tid(),
                result.resultCode(), "[INICIS MOCK] " + result.resultMessage(), result.approvedAt(), result.unknown());
    }

    @Override
    public PaymentCancelResult cancel(PaymentCancelCommand command) {
        PaymentCancelResult result = mockPaymentGateway.cancel(command);
        return new PaymentCancelResult(provider(), result.success(), result.paymentStatus(), result.resultCode(),
                "[INICIS MOCK] " + result.resultMessage(), result.unknown());
    }

    @Override
    public PaymentQueryResult query(PaymentQueryCommand command) {
        PaymentQueryResult result = mockPaymentGateway.query(command);
        return new PaymentQueryResult(provider(), result.success(), result.paymentStatus(), result.tid(),
                result.resultCode(), "[INICIS MOCK] " + result.resultMessage());
    }
}
