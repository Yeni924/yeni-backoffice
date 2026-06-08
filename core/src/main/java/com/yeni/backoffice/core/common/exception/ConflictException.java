package com.yeni.backoffice.core.common.exception;

public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
