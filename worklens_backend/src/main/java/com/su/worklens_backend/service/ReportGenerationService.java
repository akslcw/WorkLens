package com.su.worklens_backend.service;

import java.time.LocalDate;

public interface ReportGenerationService {

    void generateDailyReports(LocalDate reportDate);
}
