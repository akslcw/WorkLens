package com.su.worklens_backend.service.impl;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;
import com.su.worklens_backend.entity.DetailAccessRequest;
import com.su.worklens_backend.entity.Employee;
import com.su.worklens_backend.mapper.DetailAccessRequestMapper;
import com.su.worklens_backend.mapper.EmployeeMapper;
import com.su.worklens_backend.service.DetailAccessRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class DetailAccessRequestServiceImpl implements DetailAccessRequestService {

    private static final String STATUS_PENDING = "PENDING";

    private final DetailAccessRequestMapper detailAccessRequestMapper;
    private final EmployeeMapper employeeMapper;

    public DetailAccessRequestServiceImpl(DetailAccessRequestMapper detailAccessRequestMapper, EmployeeMapper employeeMapper) {
        this.detailAccessRequestMapper = detailAccessRequestMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public DetailAccessRequestResponse createDetailAccessRequest(DetailAccessRequestCreateRequest request, AuthenticatedUser authenticatedUser) {
        Employee targetEmployee = employeeMapper.selectById(request.getTargetEmployeeId());
        if (targetEmployee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target employee not found");
        }

        DetailAccessRequest detailAccessRequest = new DetailAccessRequest();
        detailAccessRequest.setRequesterEmployeeId(authenticatedUser.getEmployeeId());
        detailAccessRequest.setTargetEmployeeId(targetEmployee.getId());
        detailAccessRequest.setReason(request.getReason().trim());
        detailAccessRequest.setStatus(STATUS_PENDING);
        detailAccessRequest.setCreatedAt(LocalDateTime.now());
        detailAccessRequestMapper.insert(detailAccessRequest);

        return toResponse(detailAccessRequest);
    }

    private DetailAccessRequestResponse toResponse(DetailAccessRequest detailAccessRequest) {
        return new DetailAccessRequestResponse(
                detailAccessRequest.getId(),
                detailAccessRequest.getRequesterEmployeeId(),
                detailAccessRequest.getTargetEmployeeId(),
                detailAccessRequest.getReason(),
                detailAccessRequest.getStatus(),
                detailAccessRequest.getCreatedAt(),
                detailAccessRequest.getProcessedAt(),
                detailAccessRequest.getProcessedByEmployeeId()
        );
    }
}
