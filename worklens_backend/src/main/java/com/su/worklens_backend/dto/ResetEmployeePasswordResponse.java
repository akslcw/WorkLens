package com.su.worklens_backend.dto;

public class ResetEmployeePasswordResponse {

    private String username;
    private String initialPassword;
    private boolean mustChangePassword;

    public ResetEmployeePasswordResponse() {
    }

    public ResetEmployeePasswordResponse(String username, String initialPassword, boolean mustChangePassword) {
        this.username = username;
        this.initialPassword = initialPassword;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
