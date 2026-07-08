package com.su.worklens_backend.exception;

public class ManualReportGenerationDisabledException extends RuntimeException {

    public ManualReportGenerationDisabledException() {
        super("Manual report generation has been disabled. Reports are generated automatically by schedule.");
    }
}
