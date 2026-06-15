package com.yeni.backoffice.core.payment.client;

public record AlimtalkSendResult(boolean success, String providerMessageId, String message) {
}
