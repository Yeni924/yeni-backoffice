package com.yeni.backoffice.core.payment.support;

import com.yeni.backoffice.core.payment.entity.AuditLog;
import com.yeni.backoffice.core.payment.entity.PgApiLog;
import com.yeni.backoffice.core.payment.enums.LogResultStatus;
import com.yeni.backoffice.core.payment.enums.PaymentEventType;
import com.yeni.backoffice.core.payment.enums.PgApiType;
import com.yeni.backoffice.core.payment.enums.PgCompany;
import com.yeni.backoffice.core.payment.enums.PgProvider;
import com.yeni.backoffice.core.payment.repository.AuditLogRepository;
import com.yeni.backoffice.core.payment.repository.PgApiLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentAuditHelper {

    private final PgApiLogRepository pgApiLogRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentAuditHelper(PgApiLogRepository pgApiLogRepository, AuditLogRepository auditLogRepository) {
        this.pgApiLogRepository = pgApiLogRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public PgApiLog savePgLog(Long paymentId, String orderNo, PgProvider pgProvider,
                               PaymentEventType eventType, PgApiType apiType,
                               String idempotencyKey, String requestBody,
                               LogResultStatus status, String message) {
        return pgApiLogRepository.save(PgApiLog.builder()
                .requestId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .orderNo(orderNo)
                .pgCompany(PgCompany.valueOf(pgProvider.name().equals("MOCK") ? "INICIS" : pgProvider.name()))
                .pgProvider(pgProvider)
                .eventType(eventType)
                .apiType(apiType)
                .idempotencyKey(idempotencyKey)
                .requestBody(requestBody)
                .resultStatus(status)
                .resultMessage(message)
                .httpStatus(200)
                .successYn(LogResultStatus.SUCCESS.equals(status))
                .loggedAt(LocalDateTime.now())
                .build());
    }

    public PgApiLog savePgLog(Long paymentId, String orderNo, PgApiType apiType,
                               String requestBody, LogResultStatus status, String message) {
        return pgApiLogRepository.save(PgApiLog.builder()
                .requestId(UUID.randomUUID().toString())
                .paymentId(paymentId)
                .orderNo(orderNo)
                .pgCompany(PgCompany.INICIS)
                .apiType(apiType)
                .requestBody(requestBody)
                .resultStatus(status)
                .resultMessage(message)
                .loggedAt(LocalDateTime.now())
                .build());
    }

    public void saveAudit(String domainType, String actionType, String referenceKey, String description) {
        auditLogRepository.save(AuditLog.builder()
                .domainType(domainType)
                .actionType(actionType)
                .referenceKey(referenceKey)
                .description(description)
                .loggedAt(LocalDateTime.now())
                .build());
    }
}
