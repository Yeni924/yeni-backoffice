package com.yeni.backoffice.core.payment.client;

public record ExternalSendResult(boolean success, String externalResponseCode, String message) {
}
