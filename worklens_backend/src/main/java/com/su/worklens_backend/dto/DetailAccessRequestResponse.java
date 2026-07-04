package com.su.worklens_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailAccessRequestResponse {

    private Long id;
    private Long requesterEmployeeId;
    private Long targetEmployeeId;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long processedByEmployeeId;

    public DetailAccessRequestResponse() {
    }

    public DetailAccessRequestResponse(Long id, Long requesterEmployeeId, Long targetEmployeeId, String reason, String status,
                                       LocalDateTime createdAt, LocalDateTime processedAt, Long processedByEmployeeId) {
        this.id = id;
        this.requesterEmployeeId = requesterEmployeeId;
        this.targetEmployeeId = targetEmployeeId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.processedByEmployeeId = processedByEmployeeId;
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

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
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

    public Long getProcessedByEmployeeId() {
        return processedByEmployeeId;
    }

    public void setProcessedByEmployeeId(Long processedByEmployeeId) {
        this.processedByEmployeeId = processedByEmployeeId;
    }
}
