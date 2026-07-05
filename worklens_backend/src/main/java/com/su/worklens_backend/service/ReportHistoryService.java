package com.su.worklens_backend.service;

import com.su.worklens_backend.dto.ReportHistoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportHistoryService {

    void saveEmployeeWeeklyReport(Long employeeId, LocalDateTime periodStartedAt, LocalDateTime periodEndedAt, String summary);

    void saveTeamSummaryReport(Long requesterEmployeeId, String summary);

    List<ReportHistoryResponse> listEmployeeReportHistory(Long employeeId);

    List<ReportHistoryResponse> listTeamReportHistory(Long requesterEmployeeId);
}
