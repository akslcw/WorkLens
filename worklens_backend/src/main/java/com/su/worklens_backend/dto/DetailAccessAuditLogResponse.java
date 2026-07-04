package com.su.worklens_backend.dto;

import java.time.LocalDateTime;

public class DetailAccessAuditLogResponse {

    private Long id;
    private Long detailAccessRequestId;
    private Long viewerEmployeeId;
    private Long targetEmployeeId;
    private LocalDateTime viewedAt;

    public DetailAccessAuditLogResponse() {
    }

    public DetailAccessAuditLogResponse(Long id, Long detailAccessRequestId, Long viewerEmployeeId, Long targetEmployeeId, LocalDateTime viewedAt) {
        this.id = id;
        this.detailAccessRequestId = detailAccessRequestId;
        this.viewerEmployeeId = viewerEmployeeId;
        this.targetEmployeeId = targetEmployeeId;
        this.viewedAt = viewedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDetailAccessRequestId() {
        return detailAccessRequestId;
    }

    public void setDetailAccessRequestId(Long detailAccessRequestId) {
        this.detailAccessRequestId = detailAccessRequestId;
    }

    public Long getViewerEmployeeId() {
        return viewerEmployeeId;
    }

    public void setViewerEmployeeId(Long viewerEmployeeId) {
        this.viewerEmployeeId = viewerEmployeeId;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(Long targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
