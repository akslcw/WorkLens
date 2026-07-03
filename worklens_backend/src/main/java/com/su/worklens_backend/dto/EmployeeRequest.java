package com.su.worklens_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class EmployeeRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "employeeNo must not be blank")
    private String employeeNo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }
}
