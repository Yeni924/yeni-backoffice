package com.yeni.backoffice.core.payment.gateway.result;

import com.yeni.backoffice.core.payment.enums.PaymentStatus;
import com.yeni.backoffice.core.payment.enums.PgProvider;

import java.time.LocalDateTime;

public record PaymentApproveResult(
        PgProvider pgProvider,
        boolean success,
        PaymentStatus paymentStatus,
        String tid,
        String resultCode,
        String resultMessage,
        LocalDateTime approvedAt,
        boolean unknown
) {
}
