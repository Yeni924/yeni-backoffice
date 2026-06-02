package com.yeni.backoffice.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumeSummaryDto {
    private String name;
    private String role;
    private String mainStack;
    private String expandingStack;
    private String domain;
}

