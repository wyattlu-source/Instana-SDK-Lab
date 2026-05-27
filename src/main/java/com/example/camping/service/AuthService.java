package com.example.camping.service;

import com.example.camping.config.AppConfig;
import com.example.camping.dto.AuthResponse;
import com.example.camping.dto.LoginPayload;
import com.example.camping.dto.RegisterPayload;
import com.example.camping.model.User;
import com.example.camping.repository.UserRepository;
import com.example.camping.util.JwtUtil;
import com.example.camping.util.PasswordUtil;
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

    public AuthResponse register(RegisterPayload payload) {

        if (userRepository.existsByEmail(payload.getEmail())) {
            BadRequestException ex = new BadRequestException("此 Email 已被註冊");
            LOGGER.error("[AUTH] register failed - duplicate email: " + payload.getEmail(), ex);
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
        auditService.record("register", payload.getEmail());
        String token = JwtUtil.generate(appConfig.jwtSecret(), user.getUserId(), user.getEmail(), user.getName());
        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginPayload payload) {

        User user = userRepository.findByEmail(payload.getEmail())
                .orElseThrow(() -> {
                    NotAuthorizedException ex = new NotAuthorizedException("Email 或密碼錯誤");
                    LOGGER.error("[AUTH] login failed - email not found: " + payload.getEmail(), ex);
                    return ex;
                });

        if (!PasswordUtil.verify(payload.getPassword(), user.getPasswordHash())) {
            NotAuthorizedException ex = new NotAuthorizedException("Email 或密碼錯誤");
            LOGGER.error("[AUTH] login failed - wrong password for email: " + payload.getEmail(), ex);
            throw ex;
        }

        LOGGER.warn("[AUTH] login success - email: " + payload.getEmail() + " userId: " + user.getUserId());
        auditService.record("login", payload.getEmail());
        String token = JwtUtil.generate(appConfig.jwtSecret(), user.getUserId(), user.getEmail(), user.getName());
        return new AuthResponse(token, user.getUserId(), user.getName(), user.getEmail());
    }

    public Map<String, String> verifyToken(String token) {
        try {
            return JwtUtil.verify(appConfig.jwtSecret(), token);
        } catch (Exception e) {
            LOGGER.error("[AUTH] token verification failed: " + e.getMessage(), e);
            throw e;
        }
    }
}
