package com.su.worklens_backend.dto;

import java.time.LocalDate;
import java.util.List;

public class UsageViewResponse {

    private String mode;
    private LocalDate date;
    private Integer page;
    private Integer pageSize;
    private Integer totalApps;
    private List<UsageAppCardResponse> items;
    private UsageReportViewResponse report;

    public UsageViewResponse() {
    }

    public static UsageViewResponse liveUsage(LocalDate date, int page, int pageSize, int totalApps, List<UsageAppCardResponse> items) {
        UsageViewResponse response = new UsageViewResponse();
        response.setMode("LIVE_USAGE");
        response.setDate(date);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalApps(totalApps);
        response.setItems(items);
        return response;
    }

    public static UsageViewResponse report(LocalDate date, UsageReportViewResponse report) {
        UsageViewResponse response = new UsageViewResponse();
        response.setMode("REPORT");
        response.setDate(date);
        response.setReport(report);
        return response;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalApps() {
        return totalApps;
    }

    public void setTotalApps(Integer totalApps) {
        this.totalApps = totalApps;
    }

    public List<UsageAppCardResponse> getItems() {
        return items;
    }

    public void setItems(List<UsageAppCardResponse> items) {
        this.items = items;
    }

    public UsageReportViewResponse getReport() {
        return report;
    }

    public void setReport(UsageReportViewResponse report) {
        this.report = report;
    }
}
