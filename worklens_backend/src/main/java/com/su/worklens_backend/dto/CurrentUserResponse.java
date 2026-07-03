package com.su.worklens_backend.dto;

public class CurrentUserResponse {

    private Long employeeId;
    private String username;
    private String role;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(Long employeeId, String username, String role) {
        this.employeeId = employeeId;
        this.username = username;
        this.role = role;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
