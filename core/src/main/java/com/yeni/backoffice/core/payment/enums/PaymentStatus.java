package com.yeni.backoffice.core.payment.enums;

public enum PaymentStatus {
    READY,
    APPROVE_REQUESTED,
    APPROVED,
    APPROVE_FAILED,
    CANCEL_REQUESTED,
    PARTIAL_CANCELED,
    CANCELED,
    CANCEL_FAILED,
    UNKNOWN
}
