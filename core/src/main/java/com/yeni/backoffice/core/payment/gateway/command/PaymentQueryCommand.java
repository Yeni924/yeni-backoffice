package com.yeni.backoffice.core.payment.gateway.command;

import com.yeni.backoffice.core.payment.enums.PgProvider;

public record PaymentQueryCommand(
        PgProvider pgProvider,
        String tid,
        String orderNo
) {
}
