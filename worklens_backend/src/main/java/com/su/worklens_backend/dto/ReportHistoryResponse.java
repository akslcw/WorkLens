package com.su.worklens_backend.dto;

import java.time.LocalDateTime;

public class ReportHistoryResponse {

    private String reportType;
    private String summary;
    private LocalDateTime periodStartedAt;
    private LocalDateTime periodEndedAt;
    private LocalDateTime createdAt;

    public ReportHistoryResponse() {
    }

    public ReportHistoryResponse(String reportType, String summary, LocalDateTime periodStartedAt, LocalDateTime periodEndedAt, LocalDateTime createdAt) {
        this.reportType = reportType;
        this.summary = summary;
        this.periodStartedAt = periodStartedAt;
        this.periodEndedAt = periodEndedAt;
        this.createdAt = createdAt;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getPeriodStartedAt() {
        return periodStartedAt;
    }

    public void setPeriodStartedAt(LocalDateTime periodStartedAt) {
        this.periodStartedAt = periodStartedAt;
    }

    public LocalDateTime getPeriodEndedAt() {
        return periodEndedAt;
    }

    public void setPeriodEndedAt(LocalDateTime periodEndedAt) {
        this.periodEndedAt = periodEndedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
