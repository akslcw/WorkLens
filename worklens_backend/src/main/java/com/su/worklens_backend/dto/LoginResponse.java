package com.su.worklens_backend.dto;

public class LoginResponse {

    private String token;
    private String username;
    private String displayName;
    private String role;
    private boolean mustChangePassword;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String role) {
        this(token, username, username, role, false);
    }

    public LoginResponse(String token, String username, String role, boolean mustChangePassword) {
        this(token, username, username, role, mustChangePassword);
    }

    public LoginResponse(String token, String username, String displayName, String role, boolean mustChangePassword) {
        this.token = token;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
