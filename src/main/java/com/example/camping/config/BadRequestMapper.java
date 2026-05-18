package com.example.camping.config;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class BadRequestMapper implements ExceptionMapper<BadRequestException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadRequestMapper.class);

    @Override
    public Response toResponse(BadRequestException exception) {
        String message = exception.getMessage();
        String detail = message == null || message.isBlank() ? "Bad request" : message;
        LOGGER.warn("[HTTP 400] Bad Request - detail: " + detail);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("detail", detail))
                .build();
    }
}
