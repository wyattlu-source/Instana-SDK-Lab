package com.example.camping.resource;

import com.example.camping.dto.AuthResponse;
import com.example.camping.dto.LoginPayload;
import com.example.camping.dto.RegisterPayload;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.service.AuthService;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.AUTH_REGISTER_HTTP_SPAN, capturedStackFrames = 5)
    public AuthResponse register(@Valid @TagParam("register_payload") RegisterPayload payload) {
        InstanaTracing.httpEntry(InstanaTracing.AUTH_REGISTER_HTTP_SPAN, "POST", "/api/auth/register", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.AUTH_REGISTER_HTTP_SPAN, AuthResource.class.getName(), "register");
        InstanaTracing.entry(InstanaTracing.AUTH_REGISTER_HTTP_SPAN, "tags.auth.email", payload.getEmail());
        InstanaTracing.entry(InstanaTracing.AUTH_REGISTER_HTTP_SPAN, "tags.auth.name", payload.getName());
        return authService.register(payload);
    }

    @POST
    @Path("/login")
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.AUTH_LOGIN_HTTP_SPAN, capturedStackFrames = 5)
    public AuthResponse login(@Valid @TagParam("login_payload") LoginPayload payload) {
        InstanaTracing.httpEntry(InstanaTracing.AUTH_LOGIN_HTTP_SPAN, "POST", "/api/auth/login", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.AUTH_LOGIN_HTTP_SPAN, AuthResource.class.getName(), "login");
        InstanaTracing.entry(InstanaTracing.AUTH_LOGIN_HTTP_SPAN, "tags.auth.email", payload.getEmail());
        return authService.login(payload);
    }

    @POST
    @Path("/logout")
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.AUTH_LOGOUT_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, String> logout() {
        InstanaTracing.httpEntry(InstanaTracing.AUTH_LOGOUT_HTTP_SPAN, "POST", "/api/auth/logout", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.AUTH_LOGOUT_HTTP_SPAN, AuthResource.class.getName(), "logout");
        return Map.of("message", "已登出，請刪除本地 Token");
    }
}
