package com.yeni.backoffice.core.payment.gateway.result;

import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;

public record PaymentCancelResult(
        PgProvider pgProvider,
        boolean success,
        PaymentStatus paymentStatus,
        String resultCode,
        String resultMessage,
        boolean unknown
) {
}
