package com.yeni.backoffice.api.common.advice;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String timestamp,
        int status,
        String code,
        String message,
        String path,
        String requestId,
        List<FieldErrorResponse> fieldErrors
) {

    public static ErrorResponse of(int status, String code, String message, String path, String requestId) {
        return new ErrorResponse(LocalDateTime.now().toString(), status, code, message, path, requestId, null);
    }

    public static ErrorResponse of(
            int status,
            String code,
            String message,
            String path,
            String requestId,
            List<FieldErrorResponse> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now().toString(), status, code, message, path, requestId, fieldErrors);
    }

    public record FieldErrorResponse(
            String field,
            String message,
            Object rejectedValue
    ) {
    }
}
