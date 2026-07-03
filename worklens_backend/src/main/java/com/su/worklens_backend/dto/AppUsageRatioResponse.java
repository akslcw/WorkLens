package com.su.worklens_backend.dto;

import java.math.BigDecimal;

public class AppUsageRatioResponse {

    private String appName;
    private long usageMinutes;
    private BigDecimal usageRatio;

    public AppUsageRatioResponse() {
    }

    public AppUsageRatioResponse(String appName, long usageMinutes, BigDecimal usageRatio) {
        this.appName = appName;
        this.usageMinutes = usageMinutes;
        this.usageRatio = usageRatio;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getUsageMinutes() {
        return usageMinutes;
    }

    public void setUsageMinutes(long usageMinutes) {
        this.usageMinutes = usageMinutes;
    }

    public BigDecimal getUsageRatio() {
        return usageRatio;
    }

    public void setUsageRatio(BigDecimal usageRatio) {
        this.usageRatio = usageRatio;
    }
}
