package com.su.worklens_backend.dto;

public class PasswordChangeResponse {

    private String username;
    private boolean mustChangePassword;

    public PasswordChangeResponse() {
    }

    public PasswordChangeResponse(String username, boolean mustChangePassword) {
        this.username = username;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
