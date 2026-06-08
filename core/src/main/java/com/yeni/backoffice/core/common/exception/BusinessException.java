package com.yeni.backoffice.core.common.exception;

import java.util.Map;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;
    private final Map<String, Object> payload;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        this(errorCode, detailMessage, null);
    }

    public BusinessException(ErrorCode errorCode, String detailMessage, Map<String, Object> payload) {
        super(detailMessage == null || detailMessage.isBlank() ? errorCode.getDefaultMessage() : detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.payload = payload;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
