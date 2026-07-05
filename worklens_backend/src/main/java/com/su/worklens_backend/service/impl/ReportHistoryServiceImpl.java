package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.dto.ReportHistoryResponse;
import com.su.worklens_backend.entity.LlmReport;
import com.su.worklens_backend.mapper.LlmReportMapper;
import com.su.worklens_backend.service.ReportHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportHistoryServiceImpl implements ReportHistoryService {

    public static final String EMPLOYEE_WEEKLY = "EMPLOYEE_WEEKLY";
    public static final String TEAM_SUMMARY = "TEAM_SUMMARY";

    private final LlmReportMapper llmReportMapper;

    public ReportHistoryServiceImpl(LlmReportMapper llmReportMapper) {
        this.llmReportMapper = llmReportMapper;
    }

    @Override
    public void saveEmployeeWeeklyReport(Long employeeId, LocalDateTime periodStartedAt, LocalDateTime periodEndedAt, String summary) {
        LlmReport report = new LlmReport();
        report.setReportType(EMPLOYEE_WEEKLY);
        report.setRequesterEmployeeId(employeeId);
        report.setTargetEmployeeId(employeeId);
        report.setSummary(summary);
        report.setPeriodStartedAt(periodStartedAt);
        report.setPeriodEndedAt(periodEndedAt);
        report.setCreatedAt(LocalDateTime.now());
        llmReportMapper.insert(report);
    }

    @Override
    public void saveTeamSummaryReport(Long requesterEmployeeId, String summary) {
        LlmReport report = new LlmReport();
        report.setReportType(TEAM_SUMMARY);
        report.setRequesterEmployeeId(requesterEmployeeId);
        report.setTargetEmployeeId(null);
        report.setSummary(summary);
        report.setPeriodStartedAt(null);
        report.setPeriodEndedAt(null);
        report.setCreatedAt(LocalDateTime.now());
        llmReportMapper.insert(report);
    }

    @Override
    public List<ReportHistoryResponse> listEmployeeReportHistory(Long employeeId) {
        return llmReportMapper.selectList(
                        new LambdaQueryWrapper<LlmReport>()
                                .eq(LlmReport::getReportType, EMPLOYEE_WEEKLY)
                                .eq(LlmReport::getTargetEmployeeId, employeeId)
                                .orderByDesc(LlmReport::getCreatedAt, LlmReport::getId)
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ReportHistoryResponse> listTeamReportHistory(Long requesterEmployeeId) {
        return llmReportMapper.selectList(
                        new LambdaQueryWrapper<LlmReport>()
                                .eq(LlmReport::getReportType, TEAM_SUMMARY)
                                .eq(LlmReport::getRequesterEmployeeId, requesterEmployeeId)
                                .orderByDesc(LlmReport::getCreatedAt, LlmReport::getId)
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReportHistoryResponse toResponse(LlmReport report) {
        return new ReportHistoryResponse(
                report.getReportType(),
                report.getSummary(),
                report.getPeriodStartedAt(),
                report.getPeriodEndedAt(),
                report.getCreatedAt()
        );
    }
}
