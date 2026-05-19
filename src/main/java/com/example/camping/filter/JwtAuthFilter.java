package com.example.camping.filter;

import com.example.camping.model.AuthenticatedUser;
import com.example.camping.service.AuthService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final Set<String> PROTECTED = Set.of("checkout", "favorites", "orders", "coupons", "send_src_email");

    @Inject
    AuthService authService;

    @Inject
    AuthenticatedUser authenticatedUser;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath().replaceFirst("^/+", "");

        if ("OPTIONS".equalsIgnoreCase(ctx.getMethod())) return;
        if (path.startsWith("auth/") || path.equals("auth")) return;

        boolean requiresAuth = PROTECTED.stream().anyMatch(path::startsWith);
        if (!requiresAuth) return;

        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.error("[JWT] missing or malformed Authorization header - path: " + path);
            abort(ctx, "缺少或格式錯誤的 Authorization header");
            return;
        }

        try {
            Map<String, String> claims = authService.verifyToken(authHeader.substring(7));
            authenticatedUser.setUserId(claims.get("sub"));
            authenticatedUser.setUserEmail(claims.get("email"));
            authenticatedUser.setUserName(claims.get("name"));
        } catch (Exception e) {
            LOGGER.error("[JWT] token verification failed - path: " + path + " reason: " + e.getMessage());
            abort(ctx, "Token 無效或已過期");
        }
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\":\"" + message + "\"}")
                .build());
    }
}
