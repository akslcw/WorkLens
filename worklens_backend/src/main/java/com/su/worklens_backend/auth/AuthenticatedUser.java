package com.su.worklens_backend.auth;

public class AuthenticatedUser {

    private final Long authUserId;
    private final Long employeeId;
    private final String username;
    private final String role;

    public AuthenticatedUser(Long authUserId, Long employeeId, String username, String role) {
        this.authUserId = authUserId;
        this.employeeId = employeeId;
        this.username = username;
        this.role = role;
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
}
