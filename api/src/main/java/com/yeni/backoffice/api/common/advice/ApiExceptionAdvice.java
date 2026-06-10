package com.yeni.backoffice.api.common.advice;

import com.yeni.backoffice.api.common.advice.ErrorResponse.FieldErrorResponse;
import com.yeni.backoffice.core.common.exception.BusinessException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionAdvice.class);
    private static final String REQUEST_ID = "requestId";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        logBusinessException(errorCode, exception, request);
        return build(HttpStatus.valueOf(errorCode.getStatus()), errorCode.name(), exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage(), sanitizeRejectedValue(error.getRejectedValue())))
                .toList();
        log.warn("Request validation failed. code={}, method={}, path={}, requestId={}, fields={}",
                ErrorCode.VALIDATION_ERROR.name(), request.getMethod(), request.getRequestURI(), requestId(),
                fieldErrors.stream().map(FieldErrorResponse::field).toList());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(
                        String.valueOf(violation.getPropertyPath()),
                        violation.getMessage(),
                        sanitizeRejectedValue(violation.getInvalidValue())))
                .toList();
        log.warn("Constraint validation failed. code={}, method={}, path={}, requestId={}, fields={}",
                ErrorCode.VALIDATION_ERROR.name(), request.getMethod(), request.getRequestURI(), requestId(),
                fieldErrors.stream().map(FieldErrorResponse::field).toList());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        String message = "필수 요청 파라미터가 누락되었습니다: " + exception.getParameterName();
        log.warn("Required request parameter is missing. method={}, path={}, requestId={}, parameter={}",
                request.getMethod(), request.getRequestURI(), requestId(), exception.getParameterName());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), message, request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String message = "지원하지 않는 요청값입니다: " + exception.getName();
        log.warn("Request parameter type mismatch. method={}, path={}, requestId={}, parameter={}",
                request.getMethod(), request.getRequestURI(), requestId(), exception.getName());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        log.warn("Request JSON cannot be parsed. method={}, path={}, requestId={}",
                request.getMethod(), request.getRequestURI(), requestId());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), "요청 JSON을 해석할 수 없습니다.", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        log.info("Data integrity conflict. code={}, method={}, path={}, requestId={}",
                ErrorCode.DATA_INTEGRITY_VIOLATION.name(), request.getMethod(), request.getRequestURI(), requestId());
        return build(
                HttpStatus.CONFLICT,
                ErrorCode.DATA_INTEGRITY_VIOLATION.name(),
                ErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        log.warn("Invalid request argument. code={}, method={}, path={}, requestId={}, message={}",
                ErrorCode.INVALID_REQUEST.name(), request.getMethod(), request.getRequestURI(), requestId(), exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), exception.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception. method={}, path={}, requestId={}",
                request.getMethod(), request.getRequestURI(), requestId(), exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage(),
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), code, message, request.getRequestURI(), requestId(), fieldErrors));
    }

    private void logBusinessException(ErrorCode errorCode, BusinessException exception, HttpServletRequest request) {
        String message = "Business exception. code={}, method={}, path={}, requestId={}, message={}";
        if (errorCode.getStatus() >= 500) {
            log.error(message, errorCode.name(), request.getMethod(), request.getRequestURI(), requestId(), exception.getMessage(), exception);
        } else if (errorCode.getStatus() == 409) {
            log.info(message, errorCode.name(), request.getMethod(), request.getRequestURI(), requestId(), exception.getMessage());
        } else {
            log.warn(message, errorCode.name(), request.getMethod(), request.getRequestURI(), requestId(), exception.getMessage());
        }
    }

    private String requestId() {
        String requestId = MDC.get(REQUEST_ID);
        return requestId == null || requestId.isBlank() ? "-" : requestId;
    }

    private Object sanitizeRejectedValue(Object rejectedValue) {
        if (rejectedValue == null) {
            return null;
        }
        String value = String.valueOf(rejectedValue);
        return value.length() > 120 ? value.substring(0, 120) : value;
    }
}
