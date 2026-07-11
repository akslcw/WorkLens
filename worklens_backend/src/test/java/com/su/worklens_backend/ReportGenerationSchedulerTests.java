package com.su.worklens_backend;

import com.su.worklens_backend.scheduler.ReportGenerationScheduler;
import com.su.worklens_backend.service.ReportGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ReportGenerationSchedulerTests {

    @Test
    void dailyReportGenerationUsesConfiguredCronAndHongKongTimezone() throws Exception {
        Method method = ReportGenerationScheduler.class.getDeclaredMethod("generateDailyReports");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${worklens.reports.daily-cron:0 55 23 * * *}");
        assertThat(scheduled.zone()).isEqualTo("${worklens.reports.zone:Asia/Hong_Kong}");
    }

    @Test
    void dailyReportGenerationUsesCurrentDateFromClock() {
        ReportGenerationService reportGenerationService = mock(ReportGenerationService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-08T15:55:00Z"),
                ZoneId.of("Asia/Hong_Kong")
        );
        ReportGenerationScheduler scheduler = new ReportGenerationScheduler(reportGenerationService, clock);

        scheduler.generateDailyReports();

        verify(reportGenerationService).generateDailyReports(LocalDate.of(2026, 7, 8));
    }

    @Test
    void weeklyReportGenerationUsesConfiguredCronAndHongKongTimezone() throws Exception {
        Method method = ReportGenerationScheduler.class.getDeclaredMethod("generateWeeklyReports");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${worklens.reports.weekly-cron:0 55 23 * * SUN}");
        assertThat(scheduled.zone()).isEqualTo("${worklens.reports.zone:Asia/Hong_Kong}");
    }

    @Test
    void weeklyReportGenerationUsesCurrentDateFromClock() {
        ReportGenerationService reportGenerationService = mock(ReportGenerationService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-12T15:55:00Z"),
                ZoneId.of("Asia/Hong_Kong")
        );
        ReportGenerationScheduler scheduler = new ReportGenerationScheduler(reportGenerationService, clock);

        scheduler.generateWeeklyReports();

        verify(reportGenerationService).generateWeeklyReports(LocalDate.of(2026, 7, 12));
    }

    @Test
    void monthlyReportGenerationUsesConfiguredCronAndHongKongTimezone() throws Exception {
        Method method = ReportGenerationScheduler.class.getDeclaredMethod("generateMonthlyReports");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${worklens.reports.monthly-cron:0 55 23 28-31 * *}");
        assertThat(scheduled.zone()).isEqualTo("${worklens.reports.zone:Asia/Hong_Kong}");
    }

    @Test
    void monthlyReportGenerationRunsOnMonthEndDateFromClock() {
        ReportGenerationService reportGenerationService = mock(ReportGenerationService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-31T15:55:00Z"),
                ZoneId.of("Asia/Hong_Kong")
        );
        ReportGenerationScheduler scheduler = new ReportGenerationScheduler(reportGenerationService, clock);

        scheduler.generateMonthlyReports();

        verify(reportGenerationService).generateMonthlyReports(LocalDate.of(2026, 7, 31));
    }

    @Test
    void monthlyReportGenerationSkipsNonMonthEndDate() {
        ReportGenerationService reportGenerationService = mock(ReportGenerationService.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-30T15:55:00Z"),
                ZoneId.of("Asia/Hong_Kong")
        );
        ReportGenerationScheduler scheduler = new ReportGenerationScheduler(reportGenerationService, clock);

        scheduler.generateMonthlyReports();

        verify(reportGenerationService, never()).generateMonthlyReports(LocalDate.of(2026, 7, 30));
    }
}
