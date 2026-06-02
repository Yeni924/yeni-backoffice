package com.yeni.backoffice.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardStatDto {
    private String label;
    private String value;
    private String description;
    private boolean isWarning;
}

