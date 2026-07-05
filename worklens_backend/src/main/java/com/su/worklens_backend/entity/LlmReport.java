package com.su.worklens_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("llm_reports")
public class LlmReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("report_type")
    private String reportType;

    @TableField("requester_employee_id")
    private Long requesterEmployeeId;

    @TableField("target_employee_id")
    private Long targetEmployeeId;

    @TableField("summary")
    private String summary;

    @TableField("period_started_at")
    private LocalDateTime periodStartedAt;

    @TableField("period_ended_at")
    private LocalDateTime periodEndedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Long getRequesterEmployeeId() {
        return requesterEmployeeId;
    }

    public void setRequesterEmployeeId(Long requesterEmployeeId) {
        this.requesterEmployeeId = requesterEmployeeId;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
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
