package com.su.worklens_backend.controller;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.DetailAccessRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
