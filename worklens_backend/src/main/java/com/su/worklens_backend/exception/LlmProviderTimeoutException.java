package com.su.worklens_backend.exception;

public class LlmProviderTimeoutException extends LlmProviderException {

    public LlmProviderTimeoutException(String message) {
        super(message);
    }

    public LlmProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
