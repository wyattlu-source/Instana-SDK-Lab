package com.example.camping.service;

import com.example.camping.dto.OrderPayload;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrderValidateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderValidateService.class);

    public void validate(OrderPayload order) {

        if (order.getEventId() == null || order.getEventId().isBlank()) {
            BadRequestException ex = new BadRequestException("event_id is required");
            LOGGER.error("[ORDER] validation failed - event_id is required", ex);
            throw ex;
        }
        if (order.getTs() == null || order.getTs() <= 0) {
            BadRequestException ex = new BadRequestException("ts is required");
            LOGGER.error("[ORDER] validation failed - ts is required", ex);
            throw ex;
        }
        LOGGER.warn("[ORDER] validation success - event_id: " + order.getEventId() + " event_type: " + order.getEventType());
    }
}
