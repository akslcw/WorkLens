package com.su.worklens_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DetailAccessRequestCreateRequest {

    @NotNull(message = "targetEmployeeId must not be null")
    private Long targetEmployeeId;

    @NotBlank(message = "reason must not be blank")
    private String reason;

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
}
