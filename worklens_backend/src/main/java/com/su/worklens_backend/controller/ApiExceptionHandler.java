package com.su.worklens_backend.controller;

import com.su.worklens_backend.dto.ApiErrorResponse;
import com.su.worklens_backend.exception.LlmProviderException;
import com.su.worklens_backend.exception.LlmProviderTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

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
}
