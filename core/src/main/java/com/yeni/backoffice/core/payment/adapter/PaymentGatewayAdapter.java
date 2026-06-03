package com.yeni.backoffice.core.payment.adapter;

import com.yeni.backoffice.core.payment.enums.PgCompany;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentGatewayAdapter {

    PgCompany pgCompany();

    ApprovalResult approve(ApprovalRequest request);

    CancelResult cancel(CancelRequest request);

    void netCancel(NetCancelRequest request);

    record ApprovalRequest(
            String mid,
            String orderNo,
            BigDecimal amount,
            String authToken
    ) {
    }

    record ApprovalResult(
            boolean success,
            String tid,
            String resultCode,
            String resultMessage,
            LocalDateTime approvedAt
    ) {
    }

    record CancelRequest(
            String tid,
            BigDecimal cancelAmount,
            String cancelRequestKey
    ) {
    }

    record CancelResult(
            boolean success,
            String resultCode,
            String resultMessage
    ) {
    }

    record NetCancelRequest(
            String tid,
            String reason
    ) {
    }
}
