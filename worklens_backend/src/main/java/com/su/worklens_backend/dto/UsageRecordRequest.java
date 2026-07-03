package com.su.worklens_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class UsageRecordRequest {

    @NotBlank(message = "appName must not be blank")
    private String appName;

    @NotNull(message = "startedAt must not be null")
    private LocalDateTime startedAt;

    @NotNull(message = "endedAt must not be null")
    private LocalDateTime endedAt;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    @JsonIgnore
    @AssertTrue(message = "endedAt must be after startedAt")
    public boolean isTimeRangeValid() {
        if (startedAt == null || endedAt == null) {
            return true;
        }
        return endedAt.isAfter(startedAt);
    }
}
