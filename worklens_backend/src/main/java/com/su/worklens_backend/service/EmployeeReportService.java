package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.EmployeeReportResponse;

public interface EmployeeReportService {

    EmployeeReportResponse generateWeeklyReport(AuthenticatedUser authenticatedUser);
}
