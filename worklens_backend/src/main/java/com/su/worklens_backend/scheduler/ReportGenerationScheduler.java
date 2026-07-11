package com.su.worklens_backend.scheduler;

import com.su.worklens_backend.service.ReportGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class ReportGenerationScheduler {

    private final ReportGenerationService reportGenerationService;
    private final Clock clock;

    public ReportGenerationScheduler(ReportGenerationService reportGenerationService, Clock clock) {
        this.reportGenerationService = reportGenerationService;
        this.clock = clock;
    }

    @Scheduled(cron = "${worklens.reports.daily-cron:0 55 23 * * *}", zone = "${worklens.reports.zone:Asia/Hong_Kong}")
    public void generateDailyReports() {
        reportGenerationService.generateDailyReports(LocalDate.now(clock));
    }

    @Scheduled(cron = "${worklens.reports.weekly-cron:0 55 23 * * SUN}", zone = "${worklens.reports.zone:Asia/Hong_Kong}")
    public void generateWeeklyReports() {
        reportGenerationService.generateWeeklyReports(LocalDate.now(clock));
    }

    @Scheduled(cron = "${worklens.reports.monthly-cron:0 55 23 28-31 * *}", zone = "${worklens.reports.zone:Asia/Hong_Kong}")
    public void generateMonthlyReports() {
        LocalDate today = LocalDate.now(clock);
        if (today.equals(today.withDayOfMonth(today.lengthOfMonth()))) {
            reportGenerationService.generateMonthlyReports(today);
        }
    }
}
