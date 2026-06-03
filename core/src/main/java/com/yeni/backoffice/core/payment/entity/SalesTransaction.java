package com.yeni.backoffice.core.payment.entity;

import com.yeni.backoffice.core.common.entity.BaseTimeEntity;
import com.yeni.backoffice.core.payment.enums.SaleStatus;
import com.yeni.backoffice.core.payment.enums.SaleType;
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
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "sales_transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_sales_transaction_source", columnNames = {"sourceType", "sourceId"})
)
public class SalesTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String sourceType;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 80)
    private String orderNo;

    @Column(nullable = false, length = 120)
    private String tid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleType saleType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saleAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vatAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SaleStatus saleStatus;

    @Column(nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean externalSendRequired = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean settlementIncludedYn = false;

    public void markIncludedInSettlement() {
        this.settlementIncludedYn = true;
    }

    public void markSettled() {
        this.saleStatus = SaleStatus.SETTLED;
    }
}
