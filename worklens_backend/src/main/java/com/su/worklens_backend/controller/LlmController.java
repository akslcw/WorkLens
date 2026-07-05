package com.su.worklens_backend.controller;

import com.su.worklens_backend.dto.LlmTestResponse;
import com.su.worklens_backend.service.LlmProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmController {

    static final String TEST_PROMPT = "Please respond to this fixed WorkLens connectivity check text.";

    private final LlmProvider llmProvider;

    public LlmController(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @GetMapping("/llm/test-response")
    public LlmTestResponse getTestResponse() {
        return new LlmTestResponse(llmProvider.generateText(TEST_PROMPT));
    }
}
