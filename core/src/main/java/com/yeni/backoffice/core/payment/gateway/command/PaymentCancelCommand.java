package com.yeni.backoffice.core.payment.gateway.command;

import com.yeni.backoffice.core.payment.enums.PgProvider;

import java.math.BigDecimal;

public record PaymentCancelCommand(
        PgProvider pgProvider,
        String tid,
        BigDecimal cancelAmount,
        String idempotencyKey,
        String reason
) {
}
