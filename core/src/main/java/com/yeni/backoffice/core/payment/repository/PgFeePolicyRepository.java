package com.yeni.backoffice.core.payment.repository;

import com.yeni.backoffice.core.payment.entity.PgFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PgFeePolicyRepository extends JpaRepository<PgFeePolicy, Long> {

    List<PgFeePolicy> findByPgCompanyAndMidAndPaymentMethodAndUseYnTrueOrderByEffectiveStartDateAsc(
            String pgCompany,
            String mid,
            String paymentMethod
    );

    Optional<PgFeePolicy> findFirstByPgCompanyAndMidAndPaymentMethodAndUseYnTrueAndEffectiveStartDateLessThanEqualAndEffectiveEndDateGreaterThanEqual(
            String pgCompany,
            String mid,
            String paymentMethod,
            LocalDate startDate,
            LocalDate endDate
    );
}
