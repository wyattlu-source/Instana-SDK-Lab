package com.example.camping.service;

import com.example.camping.dto.SpotDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PricingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingService.class);

    @Inject
    SpotService spotService;

    public int getUnitPrice(String spotId) {
        if (spotId == null || spotId.isBlank()) return 0;
        try {
            int price = spotService.findById(spotId).map(SpotDto::getPrice).orElse(0);
            if (price == 0) LOGGER.warn("[PRICING] spot not found or price is 0 - spotId: " + spotId);
            return price;
        } catch (Exception e) {
            LOGGER.error("[PRICING] failed to get spot price - spotId: " + spotId, e);
            return 0;
        }
    }

    public int calculateTotal(String spotId, int nights) {
        int unitPrice = getUnitPrice(spotId);
        int total = unitPrice * Math.max(nights, 1);
        LOGGER.warn("[PRICING] spotId=" + spotId + " nights=" + nights + " unitPrice=" + unitPrice + " total=" + total);
        return total;
    }
}
