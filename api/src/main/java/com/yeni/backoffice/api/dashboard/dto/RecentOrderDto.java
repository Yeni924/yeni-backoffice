package com.yeni.backoffice.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecentOrderDto {
    private String orderNumber;
    private String customerName;
    private String productName;
    private String amount;
    private String status;
    private String statusClass;
    private String orderDateTime;
}

