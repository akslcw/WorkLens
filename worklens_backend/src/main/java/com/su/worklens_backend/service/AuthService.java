package com.su.worklens_backend.service;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.ChangePasswordRequest;
import com.su.worklens_backend.dto.CurrentUserResponse;
import com.su.worklens_backend.dto.LoginRequest;
import com.su.worklens_backend.dto.LoginResponse;
import com.su.worklens_backend.dto.PasswordChangeResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    AuthenticatedUser resolveAuthenticatedUser(String bearerToken);

    AuthenticatedUser getAuthenticatedUser(HttpServletRequest request);

    CurrentUserResponse getCurrentUser(HttpServletRequest request);

    PasswordChangeResponse changePassword(HttpServletRequest request, ChangePasswordRequest changePasswordRequest);
}
