package com.su.worklens_backend.controller;

import com.su.worklens_backend.dto.ApiErrorResponse;
import com.su.worklens_backend.exception.LlmProviderException;
import com.su.worklens_backend.exception.LlmProviderTimeoutException;
import com.su.worklens_backend.exception.ManualReportGenerationDisabledException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        String reason = exception.getReason() == null ? "Request failed" : exception.getReason();
        String code;
        if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                && "Invalid username or password".equals(reason)) {
            code = "INVALID_CREDENTIALS";
        } else if (exception.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            code = "LOGIN_LOCKED";
        } else {
            code = exception.getStatusCode().toString().replace(' ', '_');
        }
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiErrorResponse(code, reason));
    }

    @ExceptionHandler(LlmProviderTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiErrorResponse handleLlmTimeout(LlmProviderTimeoutException exception) {
        return new ApiErrorResponse("LLM_TIMEOUT", exception.getMessage());
    }

    @ExceptionHandler(LlmProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponse handleLlmProviderFailure(LlmProviderException exception) {
        return new ApiErrorResponse("LLM_PROVIDER_ERROR", exception.getMessage());
    }

    @ExceptionHandler(ManualReportGenerationDisabledException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiErrorResponse handleManualReportGenerationDisabled(ManualReportGenerationDisabledException exception) {
        return new ApiErrorResponse("MANUAL_REPORT_GENERATION_DISABLED", exception.getMessage());
    }
}
