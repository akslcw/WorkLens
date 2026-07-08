package com.su.worklens_backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UsageReportViewResponse {

    private String reportScope;
    private String periodType;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private String summary;
    private List<ReportDetailItemResponse> details = new ArrayList<>();

    public UsageReportViewResponse() {
    }

    public UsageReportViewResponse(String reportScope,
                                   String periodType,
                                   LocalDate periodStartDate,
                                   LocalDate periodEndDate,
                                   String summary,
                                   List<ReportDetailItemResponse> details) {
        this.reportScope = reportScope;
        this.periodType = periodType;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.summary = summary;
        this.details = details;
    }

    public String getReportScope() {
        return reportScope;
    }

    public void setReportScope(String reportScope) {
        this.reportScope = reportScope;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public void setPeriodStartDate(LocalDate periodStartDate) {
        this.periodStartDate = periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public void setPeriodEndDate(LocalDate periodEndDate) {
        this.periodEndDate = periodEndDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ReportDetailItemResponse> getDetails() {
        return details;
    }

    public void setDetails(List<ReportDetailItemResponse> details) {
        this.details = details;
    }
}
