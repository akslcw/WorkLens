package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessAuditLogResponse;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestDecisionRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.entity.DetailAccessAuditLog;
import com.su.worklens_backend.entity.DetailAccessRequest;
import com.su.worklens_backend.entity.Employee;
import com.su.worklens_backend.entity.UsageRecord;
import com.su.worklens_backend.mapper.DetailAccessAuditLogMapper;
import com.su.worklens_backend.mapper.DetailAccessRequestMapper;
import com.su.worklens_backend.mapper.EmployeeMapper;
import com.su.worklens_backend.mapper.UsageRecordMapper;
import com.su.worklens_backend.service.DetailAccessRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DetailAccessRequestServiceImpl implements DetailAccessRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_USED = "USED";
    private static final String AUTHORIZATION_USED_MESSAGE = "Detail access authorization has already been used";
    private static final String AUTHORIZATION_MISSING_MESSAGE = "Detail access authorization is expired or does not exist";

    private final DetailAccessAuditLogMapper detailAccessAuditLogMapper;
    private final DetailAccessRequestMapper detailAccessRequestMapper;
    private final EmployeeMapper employeeMapper;
    private final UsageRecordMapper usageRecordMapper;

    public DetailAccessRequestServiceImpl(DetailAccessAuditLogMapper detailAccessAuditLogMapper,
                                          DetailAccessRequestMapper detailAccessRequestMapper,
                                          EmployeeMapper employeeMapper,
                                          UsageRecordMapper usageRecordMapper) {
        this.detailAccessAuditLogMapper = detailAccessAuditLogMapper;
        this.detailAccessRequestMapper = detailAccessRequestMapper;
        this.employeeMapper = employeeMapper;
        this.usageRecordMapper = usageRecordMapper;
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

    @Override
    public DetailAccessRequestResponse decideDetailAccessRequest(Long requestId, DetailAccessRequestDecisionRequest request, AuthenticatedUser authenticatedUser) {
        DetailAccessRequest detailAccessRequest = detailAccessRequestMapper.selectById(requestId);
        if (detailAccessRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detail access request not found");
        }
        if (!authenticatedUser.getEmployeeId().equals(detailAccessRequest.getTargetEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the target employee can decide this request");
        }
        if (!STATUS_PENDING.equals(detailAccessRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Detail access request has already been processed");
        }

        String normalizedDecision = request.getDecision().trim().toUpperCase();
        if (!STATUS_APPROVED.equals(normalizedDecision) && !STATUS_REJECTED.equals(normalizedDecision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }

        detailAccessRequest.setStatus(normalizedDecision);
        detailAccessRequest.setProcessedAt(LocalDateTime.now());
        detailAccessRequest.setProcessedByEmployeeId(authenticatedUser.getEmployeeId());
        detailAccessRequestMapper.updateById(detailAccessRequest);

        return toResponse(detailAccessRequest);
    }

    @Override
    public List<DetailAccessRequestResponse> listOwnDetailAccessRequests(AuthenticatedUser authenticatedUser) {
        return detailAccessRequestMapper.selectList(
                        new LambdaQueryWrapper<DetailAccessRequest>()
                                .eq(DetailAccessRequest::getRequesterEmployeeId, authenticatedUser.getEmployeeId())
                                .orderByAsc(DetailAccessRequest::getCreatedAt, DetailAccessRequest::getId)
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<UsageRecordResponse> viewApprovedUsageRecords(Long requestId, AuthenticatedUser authenticatedUser) {
        DetailAccessRequest detailAccessRequest = detailAccessRequestMapper.selectById(requestId);
        validateViewAuthorization(detailAccessRequest, authenticatedUser);

        List<UsageRecordResponse> usageRecords = usageRecordMapper.selectList(
                        new LambdaQueryWrapper<UsageRecord>()
                                .eq(UsageRecord::getEmployeeId, detailAccessRequest.getTargetEmployeeId())
                                .orderByAsc(UsageRecord::getStartedAt, UsageRecord::getId)
                ).stream()
                .map(this::toUsageRecordResponse)
                .toList();

        DetailAccessAuditLog auditLog = new DetailAccessAuditLog();
        auditLog.setDetailAccessRequestId(detailAccessRequest.getId());
        auditLog.setViewerEmployeeId(authenticatedUser.getEmployeeId());
        auditLog.setTargetEmployeeId(detailAccessRequest.getTargetEmployeeId());
        auditLog.setViewedAt(LocalDateTime.now());
        detailAccessAuditLogMapper.insert(auditLog);

        detailAccessRequest.setStatus(STATUS_USED);
        detailAccessRequestMapper.updateById(detailAccessRequest);

        return usageRecords;
    }

    @Override
    public List<DetailAccessAuditLogResponse> listAccessAuditLogs(Long requestId, AuthenticatedUser authenticatedUser) {
        DetailAccessRequest detailAccessRequest = detailAccessRequestMapper.selectById(requestId);
        if (detailAccessRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detail access request not found");
        }
        if (!authenticatedUser.getEmployeeId().equals(detailAccessRequest.getRequesterEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requesting manager can view access audit logs");
        }

        return detailAccessAuditLogMapper.selectList(
                        new LambdaQueryWrapper<DetailAccessAuditLog>()
                                .eq(DetailAccessAuditLog::getDetailAccessRequestId, requestId)
                                .orderByAsc(DetailAccessAuditLog::getViewedAt, DetailAccessAuditLog::getId)
                ).stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    private void validateViewAuthorization(DetailAccessRequest detailAccessRequest, AuthenticatedUser authenticatedUser) {
        if (detailAccessRequest == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AUTHORIZATION_MISSING_MESSAGE);
        }
        if (!authenticatedUser.getEmployeeId().equals(detailAccessRequest.getRequesterEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AUTHORIZATION_MISSING_MESSAGE);
        }
        if (STATUS_USED.equals(detailAccessRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AUTHORIZATION_USED_MESSAGE);
        }
        if (!STATUS_APPROVED.equals(detailAccessRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AUTHORIZATION_MISSING_MESSAGE);
        }
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

    private UsageRecordResponse toUsageRecordResponse(UsageRecord usageRecord) {
        return new UsageRecordResponse(
                usageRecord.getId(),
                usageRecord.getAppName(),
                usageRecord.getStartedAt(),
                usageRecord.getEndedAt(),
                usageRecord.getCreatedAt()
        );
    }

    private DetailAccessAuditLogResponse toAuditLogResponse(DetailAccessAuditLog auditLog) {
        return new DetailAccessAuditLogResponse(
                auditLog.getId(),
                auditLog.getDetailAccessRequestId(),
                auditLog.getViewerEmployeeId(),
                auditLog.getTargetEmployeeId(),
                auditLog.getViewedAt()
        );
    }
}
