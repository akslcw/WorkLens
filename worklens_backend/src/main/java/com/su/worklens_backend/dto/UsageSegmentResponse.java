package com.su.worklens_backend.dto;

import java.time.LocalDateTime;

public class UsageSegmentResponse {

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public UsageSegmentResponse() {
    }

    public UsageSegmentResponse(LocalDateTime startedAt, LocalDateTime endedAt) {
        this.startedAt = startedAt;
        this.endedAt = endedAt;
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
}
