package com.example.camping.service;

import com.example.camping.dto.SpotDto;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PricingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingService.class);

    @Inject
    SpotService spotService;

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.PRICING_SPAN, capturedStackFrames = 5)
    public int calculateTotal(@TagParam("spot_id") String spotId, @TagParam("nights") int nights) {
        InstanaTracing.method(InstanaTracing.PRICING_SPAN, PricingService.class.getName(), "calculateTotal");
        SpanSupport.annotate("tags.pricing.spot_id", spotId == null ? "unknown" : spotId);
        SpanSupport.annotate("tags.pricing.nights", String.valueOf(nights));
        SpanSupport.annotate("tags.service", "camping-api");

        // 呼叫 spot-service 取得景點價格 → 這會產生跨服務的 span！
        int unitPrice = 0;
        if (spotId != null && !spotId.isBlank()) {
            try {
                unitPrice = spotService.findById(spotId)
                        .map(SpotDto::getPrice)
                        .orElse(0);
                if (unitPrice == 0) {
                    LOGGER.warn("[PRICING] spot not found or price is 0 - spotId: " + spotId);
                }
            } catch (Exception e) {
                LOGGER.error("[PRICING] failed to get spot price - spotId: " + spotId + " error: " + e.getMessage(), e);
                InstanaTracing.error(Span.Type.INTERMEDIATE, InstanaTracing.PRICING_SPAN, e);
                unitPrice = 0;
            }
        }

        int total = unitPrice * Math.max(nights, 1);
        SpanSupport.annotate("tags.pricing.unit_price", String.valueOf(unitPrice));
        SpanSupport.annotate("tags.pricing.total", String.valueOf(total));
        LOGGER.warn("[PRICING] spotId=" + spotId + " nights=" + nights + " unitPrice=" + unitPrice + " total=" + total);
        return total;
    }
}
