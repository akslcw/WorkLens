package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.DetailAccessRequestCreateRequest;
import com.su.worklens_backend.dto.DetailAccessRequestDecisionRequest;
import com.su.worklens_backend.dto.DetailAccessRequestResponse;

public interface DetailAccessRequestService {

    DetailAccessRequestResponse createDetailAccessRequest(DetailAccessRequestCreateRequest request, AuthenticatedUser authenticatedUser);

    DetailAccessRequestResponse decideDetailAccessRequest(Long requestId, DetailAccessRequestDecisionRequest request, AuthenticatedUser authenticatedUser);
}
