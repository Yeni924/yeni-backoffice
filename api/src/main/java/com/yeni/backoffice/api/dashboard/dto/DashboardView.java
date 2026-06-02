package com.yeni.backoffice.api.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardView {
    private ResumeSummaryDto resumeSummary;
    private List<DashboardStatDto> stats;
    private List<RecentOrderDto> recentOrders;
    private List<CareerProjectDto> careerProjects;
    private List<PlannedFeatureDto> plannedFeatures;
}

