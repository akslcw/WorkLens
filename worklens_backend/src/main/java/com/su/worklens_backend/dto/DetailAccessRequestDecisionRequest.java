package com.su.worklens_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DetailAccessRequestDecisionRequest {

    @NotBlank(message = "decision must not be blank")
    private String decision;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
