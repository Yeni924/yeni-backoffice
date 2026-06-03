package com.yeni.backoffice.core.payment.adapter;

import com.yeni.backoffice.core.payment.client.MockInicisStdPayClient;
import com.yeni.backoffice.core.payment.enums.PgCompany;
import org.springframework.stereotype.Component;

@Component
public class InicisPaymentGatewayAdapter implements PaymentGatewayAdapter {

    private final MockInicisStdPayClient inicisClient;

    public InicisPaymentGatewayAdapter(MockInicisStdPayClient inicisClient) {
        this.inicisClient = inicisClient;
    }

    @Override
    public PgCompany pgCompany() {
        return PgCompany.INICIS;
    }

    @Override
    public ApprovalResult approve(ApprovalRequest request) {
        MockInicisStdPayClient.MockApprovalResult result = inicisClient.approve(
                request.mid(),
                request.orderNo(),
                request.amount(),
                request.authToken()
        );
        return new ApprovalResult(
                result.success(),
                result.tid(),
                result.resultCode(),
                result.resultMessage(),
                result.approvedAt()
        );
    }

    @Override
    public CancelResult cancel(CancelRequest request) {
        MockInicisStdPayClient.MockCancelResult result = inicisClient.cancel(
                request.tid(),
                request.cancelAmount(),
                request.cancelRequestKey()
        );
        return new CancelResult(result.success(), result.resultCode(), result.resultMessage());
    }

    @Override
    public void netCancel(NetCancelRequest request) {
        inicisClient.netCancel(request.tid(), request.reason());
    }
}
