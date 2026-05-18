package com.example.camping.resource;

import com.example.camping.dto.SpotDto;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.service.SpotService;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
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
import org.slf4j.LoggerFactory;

@Path("/spot")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SpotResource {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SpotResource.class);

    @Inject
    SpotService spotService;

    @GET
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.SPOTS_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, Object> getSpots() {
        LOGGER.warn("[SPOT] getSpots() called");
        InstanaTracing.httpEntry(InstanaTracing.SPOTS_HTTP_SPAN, "GET", "/api/spot", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.SPOTS_HTTP_SPAN, SpotResource.class.getName(), "getSpots");
        List<SpotDto> spots = spotService.getSpots();
        LOGGER.warn("[SPOT] getSpots() returning " + spots.size() + " spots");
        return Map.of(
                "success", true,
                "message", "Spots retrieved successfully",
                "data", spots
        );
    }

    @GET
    @Path("/{spotId}")
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.SPOT_DETAIL_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, Object> getSpot(@PathParam("spotId") @TagParam("spot_id") String spotId) {
        LOGGER.warn("[SPOT] getSpot() called - spotId=" + spotId);
        InstanaTracing.httpEntry(InstanaTracing.SPOT_DETAIL_HTTP_SPAN, "GET", "/api/spot/{spotId}", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, SpotResource.class.getName(), "getSpot");
        InstanaTracing.entry(InstanaTracing.SPOT_DETAIL_HTTP_SPAN, "tags.spot.id", spotId);
        try {
            UUID.fromString(spotId);
        } catch (IllegalArgumentException e) {
            LOGGER.error("[SPOT] getSpot() invalid UUID - spotId=" + spotId, e);
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, e);
            throw new BadRequestException("Invalid spot id");
        }

        SpotDto spot = spotService.findById(spotId).orElseThrow(() -> {
            NotFoundException exception = new NotFoundException("Spot not found");
            LOGGER.warn("[SPOT] getSpot() spot not found - spotId=" + spotId);
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, exception);
            return exception;
        });

        LOGGER.warn("[SPOT] getSpot() found - spotId=" + spotId + " name=" + spot.getSpotName());
        return Map.of(
                "success", true,
                "message", "Spot retrieved successfully",
                "data", spot
        );
    }
}
