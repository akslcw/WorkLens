package com.su.worklens_backend.service;

public interface LlmProvider {

    String generateText(String prompt);
}
