package com.yeni.backoffice.core.payment.gateway.result;

import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;

public record PaymentQueryResult(
        PgProvider pgProvider,
        boolean success,
        PaymentStatus paymentStatus,
        String tid,
        String resultCode,
        String resultMessage
) {
}
