package com.example.camping.config;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class NotFoundMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("detail", message(exception, "Not found")))
                .build();
    }

    private String message(NotFoundException exception, String defaultMessage) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
