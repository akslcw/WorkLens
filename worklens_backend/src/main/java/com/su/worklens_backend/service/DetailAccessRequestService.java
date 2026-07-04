package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessAuditLogResponse;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestDecisionRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;
import com.su.worklens_backend.dto.EmployeeDetailAccessRequestResponse;
import com.su.worklens_backend.dto.UsageRecordResponse;

import java.util.List;

public interface DetailAccessRequestService {

    DetailAccessRequestResponse createDetailAccessRequest(DetailAccessRequestCreateRequest request, AuthenticatedUser authenticatedUser);

    DetailAccessRequestResponse decideDetailAccessRequest(Long requestId, DetailAccessRequestDecisionRequest request, AuthenticatedUser authenticatedUser);

    List<DetailAccessRequestResponse> listOwnDetailAccessRequests(AuthenticatedUser authenticatedUser);

    List<EmployeeDetailAccessRequestResponse> listRequestsTargetingCurrentEmployee(AuthenticatedUser authenticatedUser);

    List<UsageRecordResponse> viewApprovedUsageRecords(Long requestId, AuthenticatedUser authenticatedUser);

    List<DetailAccessAuditLogResponse> listAccessAuditLogs(Long requestId, AuthenticatedUser authenticatedUser);
}
