package com.su.worklens_backend.controller;

import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.service.UsageRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamUsageSummaryController {

    private final UsageRecordService usageRecordService;

    public TeamUsageSummaryController(UsageRecordService usageRecordService) {
        this.usageRecordService = usageRecordService;
    }

    @GetMapping("/team-usage-summary")
    public TeamUsageSummaryResponse getTeamUsageSummary() {
        return usageRecordService.getTeamUsageSummary();
    }
}
