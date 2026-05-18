package com.example.camping.config;

import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {
        // WebApplicationException (4xx) 由各自的 Mapper 處理，這裡只攔截非預期錯誤
        if (e instanceof WebApplicationException wae) {
            return wae.getResponse();
        }

        // 非預期錯誤：連線失敗、timeout、NullPointerException 等
        LOGGER.error("[UNEXPECTED] unhandled exception: " + e.getClass().getSimpleName()
                + " - " + e.getMessage(), e);

        // 標記當前 span 為錯誤
        if (SpanSupport.isTracing()) {
            SpanSupport.annotate("error.type", e.getClass().getSimpleName());
            SpanSupport.annotate("error.message", e.getMessage() != null ? e.getMessage() : "unknown");
            InstanaTracing.error(Span.Type.INTERMEDIATE, "global-exception", e);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", "Internal server error",
                        "type", e.getClass().getSimpleName(),
                        "message", e.getMessage() != null ? e.getMessage() : "unknown"
                ))
                .build();
    }
}
