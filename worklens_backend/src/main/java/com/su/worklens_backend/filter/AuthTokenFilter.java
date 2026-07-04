package com.su.worklens_backend.filter;

import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.impl.AuthServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";
    private static final String EMPLOYEES_PATH_PREFIX = "/employees";
    private static final String USAGE_RECORDS_PATH_PREFIX = "/usage-records";
    private static final String TEAM_USAGE_SUMMARY_PATH = "/team-usage-summary";
    private static final String DETAIL_ACCESS_REQUESTS_PATH_PREFIX = "/detail-access-requests";
    private static final String MANAGER_ROLE = "MANAGER";
    private static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final AuthService authService;

    public AuthTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        if (LOGIN_PATH.equals(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        AuthenticatedUser authenticatedUser = authService.resolveAuthenticatedUser(authorizationHeader);
        if (authenticatedUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        request.setAttribute(AuthServiceImpl.CURRENT_USER_ATTRIBUTE, authenticatedUser);
        if (requestPath.startsWith(EMPLOYEES_PATH_PREFIX) && !MANAGER_ROLE.equals(authenticatedUser.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Manager role required");
            return;
        }
        if (requestPath.startsWith(DETAIL_ACCESS_REQUESTS_PATH_PREFIX) && !MANAGER_ROLE.equals(authenticatedUser.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Manager role required");
            return;
        }
        if (TEAM_USAGE_SUMMARY_PATH.equals(requestPath) && !MANAGER_ROLE.equals(authenticatedUser.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Manager role required");
            return;
        }
        if (requestPath.startsWith(USAGE_RECORDS_PATH_PREFIX) && !EMPLOYEE_ROLE.equals(authenticatedUser.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Employee role required");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
