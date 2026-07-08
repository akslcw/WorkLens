package com.su.worklens_backend.controller;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.UsageRecordRequest;
import com.su.worklens_backend.dto.UsageRecordResponse;
import com.su.worklens_backend.dto.UsageViewResponse;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.UsageRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/usage-records")
public class UsageRecordController {

    private final UsageRecordService usageRecordService;
    private final AuthService authService;

    public UsageRecordController(UsageRecordService usageRecordService, AuthService authService) {
        this.usageRecordService = usageRecordService;
        this.authService = authService;
    }

    @GetMapping
    public List<UsageRecordResponse> listUsageRecords(HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return usageRecordService.listUsageRecords(authenticatedUser);
    }

    @GetMapping("/view")
    public UsageViewResponse getUsageView(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(value = "page", defaultValue = "1") int page,
                                          @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                          HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return usageRecordService.getUsageView(authenticatedUser, date, page, pageSize);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsageRecordResponse createUsageRecord(@Valid @RequestBody UsageRecordRequest request, HttpServletRequest httpServletRequest) {
        AuthenticatedUser authenticatedUser = authService.getAuthenticatedUser(httpServletRequest);
        return usageRecordService.createUsageRecord(request, authenticatedUser);
    }
}
