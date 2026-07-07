package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.dto.EmployeeRequest;
import com.su.worklens_backend.dto.ResetEmployeePasswordResponse;
import com.su.worklens_backend.entity.AuthUser;
import com.su.worklens_backend.entity.Employee;
import com.su.worklens_backend.mapper.AuthUserMapper;
import com.su.worklens_backend.mapper.EmployeeMapper;
import com.su.worklens_backend.service.EmployeeService;
import com.su.worklens_backend.service.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    public static final String INITIAL_EMPLOYEE_PASSWORD = "worklens123";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final EmployeeMapper employeeMapper;
    private final AuthUserMapper authUserMapper;
    private final PasswordHasher passwordHasher;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper, AuthUserMapper authUserMapper, PasswordHasher passwordHasher) {
        this.employeeMapper = employeeMapper;
        this.authUserMapper = authUserMapper;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setEmployeeNo(request.getEmployeeNo().trim());
        employee.setCreatedAt(LocalDateTime.now());
        employeeMapper.insert(employee);

        AuthUser authUser = new AuthUser();
        authUser.setUsername(employee.getEmployeeNo());
        // Shared initial passwords are accepted for MVP usability; must_change_password closes the normal-use window.
        authUser.setPasswordHash(passwordHasher.hash(INITIAL_EMPLOYEE_PASSWORD));
        authUser.setRole(EMPLOYEE_ROLE);
        authUser.setEmployeeId(employee.getId());
        authUser.setMustChangePassword(true);
        authUser.setCreatedAt(LocalDateTime.now());
        authUserMapper.insert(authUser);

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
        authUserMapper.delete(new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getEmployeeId, employee.getId()));
        employeeMapper.deleteById(employee.getId());
    }

    @Override
    public ResetEmployeePasswordResponse resetEmployeePassword(Long id) {
        Employee employee = getEmployeeById(id);
        AuthUser authUser = authUserMapper.selectOne(
                new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getEmployeeId, employee.getId())
        );
        if (authUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee login account not found");
        }

        authUser.setPasswordHash(passwordHasher.hash(INITIAL_EMPLOYEE_PASSWORD));
        authUser.setMustChangePassword(true);
        authUserMapper.updateById(authUser);
        return new ResetEmployeePasswordResponse(authUser.getUsername(), INITIAL_EMPLOYEE_PASSWORD, true);
    }
}
