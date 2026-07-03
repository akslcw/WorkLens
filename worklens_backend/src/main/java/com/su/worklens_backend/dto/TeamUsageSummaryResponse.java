package com.su.worklens_backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TeamUsageSummaryResponse {

    private BigDecimal teamAverageUsageMinutes;
    private long totalUsageMinutes;
    private int activeEmployeeCount;
    private List<AppUsageRatioResponse> appUsageRatios = new ArrayList<>();

    public TeamUsageSummaryResponse() {
    }

    public TeamUsageSummaryResponse(BigDecimal teamAverageUsageMinutes,
                                    long totalUsageMinutes,
                                    int activeEmployeeCount,
                                    List<AppUsageRatioResponse> appUsageRatios) {
        this.teamAverageUsageMinutes = teamAverageUsageMinutes;
        this.totalUsageMinutes = totalUsageMinutes;
        this.activeEmployeeCount = activeEmployeeCount;
        this.appUsageRatios = appUsageRatios;
    }

    public BigDecimal getTeamAverageUsageMinutes() {
        return teamAverageUsageMinutes;
    }

    public void setTeamAverageUsageMinutes(BigDecimal teamAverageUsageMinutes) {
        this.teamAverageUsageMinutes = teamAverageUsageMinutes;
    }

    public long getTotalUsageMinutes() {
        return totalUsageMinutes;
    }

    public void setTotalUsageMinutes(long totalUsageMinutes) {
        this.totalUsageMinutes = totalUsageMinutes;
    }

    public int getActiveEmployeeCount() {
        return activeEmployeeCount;
    }

    public void setActiveEmployeeCount(int activeEmployeeCount) {
        this.activeEmployeeCount = activeEmployeeCount;
    }

    public List<AppUsageRatioResponse> getAppUsageRatios() {
        return appUsageRatios;
    }

    public void setAppUsageRatios(List<AppUsageRatioResponse> appUsageRatios) {
        this.appUsageRatios = appUsageRatios;
    }
}
