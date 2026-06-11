package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "settlement_statement",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_statement_date_mid", columnNames = {"settlementDate", "mid"})
)
public class SettlementStatement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false, length = 40)
    private String pgCompany;

    @Column(nullable = false, length = 40)
    private String mid;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus settlementStatus;

    public void confirm() {
        this.settlementStatus = SettlementStatus.CONFIRMED;
    }

    public void markPaid() {
        this.settlementStatus = SettlementStatus.PAID;
    }

    public void recalculate(
            BigDecimal grossAmount,
            BigDecimal feeAmount,
            BigDecimal vatAmount,
            BigDecimal netAmount) {
        this.grossAmount = grossAmount;
        this.feeAmount = feeAmount;
        this.vatAmount = vatAmount;
        this.netAmount = netAmount;
    }
}
