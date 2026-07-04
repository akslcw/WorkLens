package com.su.worklens_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("detail_access_audit_logs")
public class DetailAccessAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("detail_access_request_id")
    private Long detailAccessRequestId;

    @TableField("viewer_employee_id")
    private Long viewerEmployeeId;

    @TableField("target_employee_id")
    private Long targetEmployeeId;

    @TableField("viewed_at")
    private LocalDateTime viewedAt;

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
