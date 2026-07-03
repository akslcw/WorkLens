package com.su.worklens_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.su.worklens_backend.auth.AuthenticatedUser;
import com.su.worklens_backend.dto.CurrentUserResponse;
import com.su.worklens_backend.dto.LoginRequest;
import com.su.worklens_backend.dto.LoginResponse;
import com.su.worklens_backend.entity.AuthToken;
import com.su.worklens_backend.entity.AuthUser;
import com.su.worklens_backend.mapper.AuthTokenMapper;
import com.su.worklens_backend.mapper.AuthUserMapper;
import com.su.worklens_backend.service.AuthService;
import com.su.worklens_backend.service.PasswordHasher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";
    private static final int TOKEN_TTL_HOURS = 24;

    private final AuthUserMapper authUserMapper;
    private final AuthTokenMapper authTokenMapper;
    private final PasswordHasher passwordHasher;

    public AuthServiceImpl(AuthUserMapper authUserMapper, AuthTokenMapper authTokenMapper, PasswordHasher passwordHasher) {
        this.authUserMapper = authUserMapper;
        this.authTokenMapper = authTokenMapper;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthUser authUser = authUserMapper.selectOne(
                new LambdaQueryWrapper<AuthUser>().eq(AuthUser::getUsername, request.getUsername().trim())
        );

        if (authUser == null || !passwordHasher.matches(request.getPassword(), authUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (authUser.getEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not bound to an employee");
        }

        AuthToken authToken = new AuthToken();
        authToken.setUserId(authUser.getId());
        authToken.setToken(UUID.randomUUID().toString().replace("-", ""));
        authToken.setCreatedAt(LocalDateTime.now());
        authToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_TTL_HOURS));
        authTokenMapper.insert(authToken);

        return new LoginResponse(authToken.getToken(), authUser.getUsername(), authUser.getRole());
    }

    @Override
    public AuthenticatedUser resolveAuthenticatedUser(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return null;
        }

        String tokenValue = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
        if (tokenValue.isEmpty()) {
            return null;
        }

        AuthToken authToken = authTokenMapper.selectOne(
                new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getToken, tokenValue)
        );

        if (authToken == null || authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        AuthUser authUser = authUserMapper.selectById(authToken.getUserId());
        if (authUser == null) {
            return null;
        }
        if (authUser.getEmployeeId() == null) {
            return null;
        }

        return new AuthenticatedUser(authUser.getId(), authUser.getEmployeeId(), authUser.getUsername(), authUser.getRole());
    }

    @Override
    public AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        Object attribute = request.getAttribute(CURRENT_USER_ATTRIBUTE);
        if (!(attribute instanceof AuthenticatedUser authenticatedUser)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authenticatedUser;
    }

    @Override
    public CurrentUserResponse getCurrentUser(HttpServletRequest request) {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser(request);

        return new CurrentUserResponse(
                authenticatedUser.getEmployeeId(),
                authenticatedUser.getUsername(),
                authenticatedUser.getRole()
        );
    }
}
