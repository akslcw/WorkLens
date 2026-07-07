package com.su.worklens_backend.auth;

public class AuthenticatedUser {

    private final Long authUserId;
    private final Long employeeId;
    private final String username;
    private final String role;
    private final boolean mustChangePassword;

    public AuthenticatedUser(Long authUserId, Long employeeId, String username, String role) {
        this(authUserId, employeeId, username, role, false);
    }

    public AuthenticatedUser(Long authUserId, Long employeeId, String username, String role, boolean mustChangePassword) {
        this.authUserId = authUserId;
        this.employeeId = employeeId;
        this.username = username;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public Long getAuthUserId() {
        return authUserId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
