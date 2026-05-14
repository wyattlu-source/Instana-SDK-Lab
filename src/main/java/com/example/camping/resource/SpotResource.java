package com.example.camping.resource;

import com.example.camping.dto.SpotDto;
import com.example.camping.service.SpotService;
import com.example.camping.util.InstanaTracingUtil;
import com.instana.sdk.annotation.Span;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Spot Resource
 *
 * 追蹤層級：
 * 1. Instana Agent 自動追蹤：HTTP 請求、方法調用
 * 2. SDK 手動追蹤：查詢參數、結果數量、業務邏輯
 */
@Path("/spot")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SpotResource {
    private static final Logger LOGGER = Logger.getLogger(SpotResource.class.getName());

    @Inject
    SpotService spotService;

    @GET
    @Span(value = "spot.getAll", type = Span.Type.ENTRY)
    public Map<String, Object> getSpots() {
        return InstanaTracingUtil.trace("SpotResource.getSpots", () -> {
            InstanaTracingUtil.markStep("1.query_spots", "查詢所有露營地點");
            
            List<SpotDto> spots = InstanaTracingUtil.trace(
                "SpotResource.callSpotService",
                () -> spotService.getSpots()
            );
            
            InstanaTracingUtil.addBusinessTag("spots.count", spots.size());
            InstanaTracingUtil.logBusinessEvent("SPOTS_QUERIED",
                String.format("Retrieved %d spots", spots.size()));
            
            return Map.of(
                "success", true,
                "message", "Spots retrieved successfully",
                "data", spots
            );
        });
    }

    @GET
    @Path("/{spotId}")
    @Span(value = "spot.getById", type = Span.Type.ENTRY)
    public Map<String, Object> getSpot(@PathParam("spotId") String spotId) {
        return InstanaTracingUtil.trace("SpotResource.getSpot", () -> {
            InstanaTracingUtil.markStep("1.validate_uuid", "驗證 Spot ID 格式");
            InstanaTracingUtil.addBusinessTag("spot.id", spotId);
            
            InstanaTracingUtil.traceVoid("SpotResource.validateUUID", () -> {
                try {
                    UUID.fromString(spotId);
                    InstanaTracingUtil.addBusinessTag("uuid.valid", true);
                } catch (IllegalArgumentException e) {
                    InstanaTracingUtil.addBusinessTag("uuid.valid", false);
                    InstanaTracingUtil.logBusinessEvent("VALIDATION_ERROR",
                        "Invalid UUID format: " + spotId);
                    throw new BadRequestException("Invalid spot id");
                }
            });
            
            InstanaTracingUtil.markStep("2.find_spot", "查詢特定露營地點");
            
            SpotDto spot = InstanaTracingUtil.trace("SpotResource.findSpotById", () ->
                spotService.findById(spotId)
                    .orElseThrow(() -> {
                        InstanaTracingUtil.addBusinessTag("spot.found", false);
                        InstanaTracingUtil.logBusinessEvent("SPOT_NOT_FOUND",
                            "Spot not found: " + spotId);
                        return new NotFoundException("Spot not found");
                    })
            );
            
            InstanaTracingUtil.addBusinessTag("spot.found", true);
            InstanaTracingUtil.addBusinessTag("spot.name", spot.getSpotName());
            InstanaTracingUtil.addBusinessTag("spot.price", spot.getPrice());
            
            InstanaTracingUtil.logBusinessEvent("SPOT_RETRIEVED",
                String.format("Spot found: %s (Price: %d)", spot.getSpotName(), spot.getPrice()));
            
            return Map.of(
                "success", true,
                "message", "Spot retrieved successfully",
                "data", spot
            );
        });
    }
}
