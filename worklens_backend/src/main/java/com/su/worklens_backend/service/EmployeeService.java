package com.su.worklens_backend.service;

import com.su.worklens_backend.dto.EmployeeRequest;
import com.su.worklens_backend.dto.ResetEmployeePasswordResponse;
import com.su.worklens_backend.entity.Employee;

import java.util.List;

public interface EmployeeService {

    Employee createEmployee(EmployeeRequest request);

    List<Employee> listEmployees();

    Employee getEmployeeById(Long id);

    Employee updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);

    ResetEmployeePasswordResponse resetEmployeePassword(Long id);
}
