package com.su.worklens_backend.dto;

import java.util.ArrayList;
import java.util.List;

public class UsageAppCardResponse {

    private String appName;
    private long durationSeconds;
    private List<UsageSegmentResponse> segments = new ArrayList<>();

    public UsageAppCardResponse() {
    }

    public UsageAppCardResponse(String appName, long durationSeconds, List<UsageSegmentResponse> segments) {
        this.appName = appName;
        this.durationSeconds = durationSeconds;
        this.segments = segments;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public List<UsageSegmentResponse> getSegments() {
        return segments;
    }

    public void setSegments(List<UsageSegmentResponse> segments) {
        this.segments = segments;
    }
}
