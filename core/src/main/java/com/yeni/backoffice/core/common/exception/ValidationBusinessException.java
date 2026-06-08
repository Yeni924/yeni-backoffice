package com.yeni.backoffice.core.common.exception;

public class ValidationBusinessException extends BusinessException {

    public ValidationBusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ValidationBusinessException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
