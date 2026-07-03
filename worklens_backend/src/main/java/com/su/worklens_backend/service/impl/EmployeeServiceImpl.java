package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.dto.EmployeeRequest;
import com.su.worklens_backend.entity.Employee;
import com.su.worklens_backend.mapper.EmployeeMapper;
import com.su.worklens_backend.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setEmployeeNo(request.getEmployeeNo().trim());
        employee.setCreatedAt(LocalDateTime.now());
        employeeMapper.insert(employee);
        return employee;
    }

    @Override
    public List<Employee> listEmployees() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Employee::getId);
        return employeeMapper.selectList(queryWrapper);
    }

    @Override
    public Employee getEmployeeById(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        }
        return employee;
    }

    @Override
    public Employee updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = getEmployeeById(id);
        employee.setName(request.getName().trim());
        employee.setEmployeeNo(request.getEmployeeNo().trim());
        employeeMapper.updateById(employee);
        return getEmployeeById(id);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employeeMapper.deleteById(employee.getId());
    }
}
