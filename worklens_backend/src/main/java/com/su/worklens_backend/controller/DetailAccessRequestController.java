package com.su.worklens_backend.controller;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessAuditLogResponse;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestDecisionRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;
import com.su.worklens_backend.dto.EmployeeDetailAccessRequestResponse;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.dto.UsageViewResponse;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.DetailAccessRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/detail-access-requests")
public class DetailAccessRequestController {

    private final DetailAccessRequestService detailAccessRequestService;
    private final AuthService authService;

    public DetailAccessRequestController(DetailAccessRequestService detailAccessRequestService, AuthService authService) {
        this.detailAccessRequestService = detailAccessRequestService;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DetailAccessRequestResponse createDetailAccessRequest(@Valid @RequestBody DetailAccessRequestCreateRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.createDetailAccessRequest(request, authenticatedUser);
    }

    @GetMapping
    public List<DetailAccessRequestResponse> listOwnDetailAccessRequests(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.listOwnDetailAccessRequests(authenticatedUser);
    }

    @GetMapping("/targeting-me")
    public List<EmployeeDetailAccessRequestResponse> listRequestsTargetingCurrentEmployee(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.listRequestsTargetingCurrentEmployee(authenticatedUser);
    }

    @PatchMapping("/{id}/decision")
    public DetailAccessRequestResponse decideDetailAccessRequest(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody DetailAccessRequestDecisionRequest request,
                                                                 HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.decideDetailAccessRequest(id, request, authenticatedUser);
    }

    @GetMapping("/{id}/usage-records")
    public List<UsageRecordResponse> viewApprovedUsageRecords(@PathVariable("id") Long id,
                                                              HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.viewApprovedUsageRecords(id, authenticatedUser);
    }

    @GetMapping("/{id}/usage-view")
    public UsageViewResponse viewApprovedUsageView(@PathVariable("id") Long id,
                                                   @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.viewApprovedUsageView(id, date, page, pageSize, authenticatedUser);
    }

    @GetMapping("/{id}/access-logs")
    public List<DetailAccessAuditLogResponse> listAccessAuditLogs(@PathVariable("id") Long id,
                                                                  HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return detailAccessRequestService.listAccessAuditLogs(id, authenticatedUser);
    }
}
