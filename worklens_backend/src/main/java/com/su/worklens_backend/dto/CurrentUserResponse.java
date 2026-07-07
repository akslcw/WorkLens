package com.su.worklens_backend.dto;

public class CurrentUserResponse {

    private Long employeeId;
    private String username;
    private String displayName;
    private String role;
    private boolean mustChangePassword;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(Long employeeId, String username, String role) {
        this(employeeId, username, username, role, false);
    }

    public CurrentUserResponse(Long employeeId, String username, String role, boolean mustChangePassword) {
        this(employeeId, username, username, role, mustChangePassword);
    }

    public CurrentUserResponse(Long employeeId, String username, String displayName, String role, boolean mustChangePassword) {
        this.employeeId = employeeId;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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
