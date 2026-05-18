package com.example.camping.service;

import com.example.camping.dto.OrderPayload;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrderValidateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderValidateService.class);

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.ORDER_VALIDATE_SPAN, capturedStackFrames = 5)
    public void validate(@TagParam("order") OrderPayload order) {
        InstanaTracing.method(InstanaTracing.ORDER_VALIDATE_SPAN, OrderValidateService.class.getName(), "validate");
        SpanSupport.annotate("tags.order.event_type", order.getEventType());
        SpanSupport.annotate("tags.order.event_id", order.getEventId());
        SpanSupport.annotate("tags.order.session_id", order.getSessionId());
        SpanSupport.annotate("tags.service", "camping-api");

        if (order.getEventId() == null || order.getEventId().isBlank()) {
            BadRequestException ex = new BadRequestException("event_id is required");
            LOGGER.error("[ORDER] validation failed - event_id is required", ex);
            InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.ORDER_VALIDATE_SPAN, ex);
            throw ex;
        }
        if (order.getTs() == null || order.getTs() <= 0) {
            BadRequestException ex = new BadRequestException("ts is required");
            LOGGER.error("[ORDER] validation failed - ts is required", ex);
            InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.ORDER_VALIDATE_SPAN, ex);
            throw ex;
        }
        LOGGER.warn("[ORDER] validation success - event_id: " + order.getEventId() + " event_type: " + order.getEventType());
        SpanSupport.annotate("tags.order.valid", "true");
    }
}
