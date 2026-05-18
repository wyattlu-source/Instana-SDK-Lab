package com.example.camping.config;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class UnauthorizedMapper implements ExceptionMapper<NotAuthorizedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnauthorizedMapper.class);

    @Override
    public Response toResponse(NotAuthorizedException e) {
        String error = e.getMessage() != null ? e.getMessage() : "認證失敗";
        LOGGER.warn("[HTTP 401] Unauthorized - error: " + error);
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", error))
                .build();
    }
}
