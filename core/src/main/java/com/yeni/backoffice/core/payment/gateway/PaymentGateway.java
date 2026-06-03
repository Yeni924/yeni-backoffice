package com.yeni.backoffice.core.payment.gateway;

import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.gateway.command.PaymentApproveCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentCancelCommand;
import com.yeni.backoffice.core.payment.gateway.command.PaymentQueryCommand;
import com.yeni.backoffice.core.payment.gateway.result.PaymentApproveResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentCancelResult;
import com.yeni.backoffice.core.payment.gateway.result.PaymentQueryResult;

public interface PaymentGateway {

    PgProvider provider();

    PaymentApproveResult approve(PaymentApproveCommand command);

    PaymentCancelResult cancel(PaymentCancelCommand command);

    PaymentQueryResult query(PaymentQueryCommand command);
}
