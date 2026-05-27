package com.example.camping.service;

import com.example.camping.config.AppConfig;
import com.example.camping.dto.AuthResponse;
import com.example.camping.dto.LoginPayload;
import com.example.camping.dto.RegisterPayload;
import com.example.camping.model.User;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.repository.UserRepository;
import com.example.camping.util.JwtUtil;
import com.example.camping.util.PasswordUtil;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    AppConfig appConfig;

    @Inject
    AuditService auditService;

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.AUTH_REGISTER_SERVICE_SPAN, capturedStackFrames = 5)
    public AuthResponse register(@TagParam("payload") RegisterPayload payload) {
        InstanaTracing.method(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_REGISTER_SERVICE_SPAN, AuthService.class.getName(), "register");
        InstanaTracing.intermediate(InstanaTracing.AUTH_REGISTER_SERVICE_SPAN, "tags.auth.email", payload.getEmail());

        if (userRepository.existsByEmail(payload.getEmail())) {
            BadRequestException ex = new BadRequestException("此 Email 已被註冊");
            LOGGER.error("[AUTH] register failed - duplicate email: " + payload.getEmail(), ex);
            InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_REGISTER_SERVICE_SPAN, ex);
            throw ex;
        }

        User user = new User(
                UUID.randomUUID().toString(),
                payload.getName(),
                payload.getEmail(),
                PasswordUtil.hash(payload.getPassword()),
                System.currentTimeMillis()
        );
        userRepository.save(user);

        LOGGER.warn("[AUTH] register success - email: " + payload.getEmail() + " userId: " + user.getUserId());
        InstanaTracing.logWarn(LOGGER, "[AUTH] user registered - email: " + payload.getEmail() + " userId: " + user.getUserId());
        InstanaTracing.intermediate(InstanaTracing.AUTH_REGISTER_SERVICE_SPAN, "tags.auth.user_id", user.getUserId());
        auditService.record("register", payload.getEmail());
        String token = JwtUtil.generate(appConfig.jwtSecret(), user.getUserId(), user.getEmail(), user.getName());
        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail());
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, capturedStackFrames = 5)
    public AuthResponse login(@TagParam("payload") LoginPayload payload) {
        InstanaTracing.method(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, AuthService.class.getName(), "login");
        InstanaTracing.intermediate(InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, "tags.auth.email", payload.getEmail());

        User user = userRepository.findByEmail(payload.getEmail())
                .orElseThrow(() -> {
                    NotAuthorizedException ex = new NotAuthorizedException("Email 或密碼錯誤");
                    LOGGER.error("[AUTH] login failed - email not found: " + payload.getEmail(), ex);
                    InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, ex);
                    return ex;
                });

        if (!PasswordUtil.verify(payload.getPassword(), user.getPasswordHash())) {
            NotAuthorizedException ex = new NotAuthorizedException("Email 或密碼錯誤");
            LOGGER.error("[AUTH] login failed - wrong password for email: " + payload.getEmail(), ex);
            InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, ex);
            throw ex;
        }

        LOGGER.warn("[AUTH] login success - email: " + payload.getEmail() + " userId: " + user.getUserId());
        InstanaTracing.logWarn(LOGGER, "[AUTH] user logged in - email: " + payload.getEmail() + " userId: " + user.getUserId());
        InstanaTracing.intermediate(InstanaTracing.AUTH_LOGIN_SERVICE_SPAN, "tags.auth.user_id", user.getUserId());
        auditService.record("login", payload.getEmail());
        String token = JwtUtil.generate(appConfig.jwtSecret(), user.getUserId(), user.getEmail(), user.getName());
        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail());
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.AUTH_VERIFY_TOKEN_SPAN, capturedStackFrames = 5)
    public Map<String, String> verifyToken(String token) {
        InstanaTracing.method(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_VERIFY_TOKEN_SPAN, AuthService.class.getName(), "verifyToken");
        try {
            return JwtUtil.verify(appConfig.jwtSecret(), token);
        } catch (Exception e) {
            LOGGER.error("[AUTH] token verification failed: " + e.getMessage(), e);
            InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.AUTH_VERIFY_TOKEN_SPAN, e);
            throw e;
        }
    }
}
