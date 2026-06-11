package com.yeni.backoffice.core.payment.service;

import com.yeni.backoffice.core.common.exception.ConflictException;
import com.yeni.backoffice.core.common.exception.ErrorCode;
import com.yeni.backoffice.core.common.exception.ValidationBusinessException;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgFeePolicyRequest;
import com.yeni.backoffice.core.payment.dto.PaymentDtos.PgFeePolicyResponse;
import com.yeni.backoffice.core.payment.entity.PgFeePolicy;
import com.yeni.backoffice.core.payment.repository.PgFeePolicyRepository;
import com.yeni.backoffice.core.payment.support.PaymentDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PgFeePolicyService {

    private final PgFeePolicyRepository feePolicyRepository;

    public PgFeePolicyService(PgFeePolicyRepository feePolicyRepository) {
        this.feePolicyRepository = feePolicyRepository;
    }

    @Transactional(readOnly = true)
    public List<PgFeePolicyResponse> getPolicies(String pgCompany, String mid, String paymentMethod) {
        return feePolicyRepository.findByPgCompanyAndMidAndPaymentMethodAndUseYnTrueOrderByEffectiveStartDateAsc(
                        defaultText(pgCompany, "INICIS"),
                        defaultText(mid, "INIpayTest"),
                        defaultText(paymentMethod, PaymentDefaults.PAYMENT_METHOD_CARD)
                ).stream()
                .map(PgFeePolicyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PgFeePolicyResponse createPolicy(PgFeePolicyRequest request) {
        validatePolicyRequest(request);
        validateOverlappedPolicy(request);

        PgFeePolicy policy = PgFeePolicy.builder()
                .pgCompany(request.pgCompany().trim())
                .mid(request.mid().trim())
                .paymentMethod(request.paymentMethod().trim())
                .feeRate(request.feeRate())
                .effectiveStartDate(request.effectiveStartDate())
                .effectiveEndDate(request.effectiveEndDate())
                .useYn(true)
                .build();
        return PgFeePolicyResponse.from(feePolicyRepository.save(policy));
    }

    private void validatePolicyRequest(PgFeePolicyRequest request) {
        if (request == null || !StringUtils.hasText(request.pgCompany()) || !StringUtils.hasText(request.mid())
                || !StringUtils.hasText(request.paymentMethod()) || request.feeRate() == null
                || request.feeRate().compareTo(BigDecimal.ZERO) < 0 || request.effectiveStartDate() == null
                || request.effectiveEndDate() == null || request.effectiveStartDate().isAfter(request.effectiveEndDate())) {
            throw new ValidationBusinessException(ErrorCode.SETTLEMENT_FEE_POLICY_INVALID);
        }
    }

    private void validateOverlappedPolicy(PgFeePolicyRequest request) {
        boolean overlapped = feePolicyRepository
                .findByPgCompanyAndMidAndPaymentMethodAndUseYnTrueOrderByEffectiveStartDateAsc(
                        request.pgCompany(),
                        request.mid(),
                        request.paymentMethod()
                ).stream()
                .anyMatch(policy -> !request.effectiveEndDate().isBefore(policy.getEffectiveStartDate())
                        && !request.effectiveStartDate().isAfter(policy.getEffectiveEndDate()));

        if (overlapped) {
            throw new ConflictException(ErrorCode.SETTLEMENT_FEE_POLICY_INVALID, "수수료 정책 적용 기간이 기존 정책과 겹칩니다.");
        }
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
