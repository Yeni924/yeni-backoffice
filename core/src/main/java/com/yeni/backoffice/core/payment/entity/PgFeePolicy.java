package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pg_fee_policy")
public class PgFeePolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String pgCompany;

    @Column(nullable = false, length = 40)
    private String mid;

    @Column(nullable = false, length = 40)
    private String paymentMethod;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal feeRate;

    @Column(nullable = false)
    private LocalDate effectiveStartDate;

    @Column(nullable = false)
    private LocalDate effectiveEndDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean useYn = true;
}
