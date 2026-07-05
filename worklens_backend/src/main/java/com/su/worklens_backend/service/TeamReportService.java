package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.TeamReportResponse;

public interface TeamReportService {

    TeamReportResponse generateTeamReport(AuthenticatedUser authenticatedUser);
}
