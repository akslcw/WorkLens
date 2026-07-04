package com.su.worklens_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDetailAccessRequestResponse {

    private Long id;
    private Long requesterEmployeeId;
    private String requesterEmployeeName;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private boolean hasBeenViewed;
    private LocalDateTime viewedAt;

    public EmployeeDetailAccessRequestResponse() {
    }

    public EmployeeDetailAccessRequestResponse(Long id, Long requesterEmployeeId, String requesterEmployeeName, String reason,
                                               String status, LocalDateTime createdAt, LocalDateTime processedAt,
                                               boolean hasBeenViewed, LocalDateTime viewedAt) {
        this.id = id;
        this.requesterEmployeeId = requesterEmployeeId;
        this.requesterEmployeeName = requesterEmployeeName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.hasBeenViewed = hasBeenViewed;
        this.viewedAt = viewedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequesterEmployeeId() {
        return requesterEmployeeId;
    }

    public void setRequesterEmployeeId(Long requesterEmployeeId) {
        this.requesterEmployeeId = requesterEmployeeId;
    }

    public String getRequesterEmployeeName() {
        return requesterEmployeeName;
    }

    public void setRequesterEmployeeName(String requesterEmployeeName) {
        this.requesterEmployeeName = requesterEmployeeName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public boolean isHasBeenViewed() {
        return hasBeenViewed;
    }

    public void setHasBeenViewed(boolean hasBeenViewed) {
        this.hasBeenViewed = hasBeenViewed;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
