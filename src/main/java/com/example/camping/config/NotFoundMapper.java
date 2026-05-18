package com.example.camping.config;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.logging.Logger;

@Provider
public class NotFoundMapper implements ExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = Logger.getLogger(NotFoundMapper.class.getName());

    @Override
    public Response toResponse(NotFoundException exception) {
        String detail = message(exception, "Not found");
        LOGGER.warning("[HTTP 404] Not Found - detail: " + detail);
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("detail", detail))
                .build();
    }

    private String message(NotFoundException exception, String defaultMessage) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? defaultMessage : message;
    }
}
