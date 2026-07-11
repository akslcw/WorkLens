package com.su.worklens_backend.dto;

import com.su.worklens_backend.entity.Employee;

import java.time.LocalDateTime;

public class CreateEmployeeResponse {

    private final Long id;
    private final String name;
    private final String employeeNo;
    private final LocalDateTime createdAt;
    private final String initialPassword;
    private final boolean mustChangePassword;

    public CreateEmployeeResponse(Employee employee, String initialPassword) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.employeeNo = employee.getEmployeeNo();
        this.createdAt = employee.getCreatedAt();
        this.initialPassword = initialPassword;
        this.mustChangePassword = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
