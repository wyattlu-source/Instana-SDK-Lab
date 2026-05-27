package com.example.camping.resource;

import com.example.camping.dto.AuthResponse;
import com.example.camping.dto.LoginPayload;
import com.example.camping.dto.RegisterPayload;
import com.example.camping.service.AuthService;
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
    public AuthResponse register(@Valid RegisterPayload payload) {
        return authService.register(payload);
    }

    @POST
    @Path("/login")
    public AuthResponse login(@Valid LoginPayload payload) {
        return authService.login(payload);
    }

    @POST
    @Path("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "已登出，請刪除本地 Token");
    }
}
