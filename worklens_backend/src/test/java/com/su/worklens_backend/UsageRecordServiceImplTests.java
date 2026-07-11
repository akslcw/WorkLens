package com.su.worklens_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.worklens_backend.dto.TeamUsageSummaryResponse;
import com.su.worklens_backend.mapper.UsageRecordMapper;
import com.su.worklens_backend.service.impl.UsageRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageRecordServiceImplTests {

    @Test
    void teamSummaryUsesDatabaseAggregatesWithoutLoadingUsageRecords() {
        UsageRecordMapper usageRecordMapper = mock(UsageRecordMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(Map.of(
                "total_usage_minutes", 135L,
                "active_employee_count", 2L
        ));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("app_name", "Slack", "usage_minutes", 75L),
                Map.of("app_name", "Chrome", "usage_minutes", 60L)
        ));
        UsageRecordServiceImpl service = new UsageRecordServiceImpl(
                usageRecordMapper,
                jdbcTemplate,
                new ObjectMapper()
        );

        TeamUsageSummaryResponse response = service.getTeamUsageSummary();

        assertThat(response.getTotalUsageMinutes()).isEqualTo(135L);
        assertThat(response.getActiveEmployeeCount()).isEqualTo(2);
        assertThat(response.getTeamAverageUsageMinutes()).isEqualByComparingTo("67.5");
        assertThat(response.getAppUsageRatios()).extracting("appName")
                .containsExactly("Slack", "Chrome");
        verify(usageRecordMapper, never()).selectList(null);
    }
}
