package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.dto.UsageRecordRequest;
import com.su.worklens_backend.dto.UsageRecordResponse;

import java.util.List;

public interface UsageRecordService {

    List<UsageRecordResponse> listUsageRecords(AuthenticatedUser authenticatedUser);

    UsageRecordResponse createUsageRecord(UsageRecordRequest request, AuthenticatedUser authenticatedUser);

    TeamUsageSummaryResponse getTeamUsageSummary();
}
