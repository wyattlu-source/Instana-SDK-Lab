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

import java.util.Map;
import java.util.UUID;

@Path("/spot")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class SpotResource {
    @Inject
    SpotService spotService;

    @GET
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.SPOTS_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, Object> getSpots() {
        InstanaTracing.httpEntry(InstanaTracing.SPOTS_HTTP_SPAN, "GET", "/api/spot", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.SPOTS_HTTP_SPAN, SpotResource.class.getName(), "getSpots");
        return Map.of(
                "success", true,
                "message", "Spots retrieved successfully",
                "data", spotService.getSpots()
        );
    }

    @GET
    @Path("/{spotId}")
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.SPOT_DETAIL_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, Object> getSpot(@PathParam("spotId") @TagParam("spot_id") String spotId) {
        InstanaTracing.httpEntry(InstanaTracing.SPOT_DETAIL_HTTP_SPAN, "GET", "/api/spot/{spotId}", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, SpotResource.class.getName(), "getSpot");
        InstanaTracing.entry(InstanaTracing.SPOT_DETAIL_HTTP_SPAN, "tags.spot.id", spotId);
        try {
            UUID.fromString(spotId);
        } catch (IllegalArgumentException e) {
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, e);
            throw new BadRequestException("Invalid spot id");
        }

        SpotDto spot = spotService.findById(spotId).orElseThrow(() -> {
            NotFoundException exception = new NotFoundException("Spot not found");
            InstanaTracing.error(Span.Type.ENTRY, InstanaTracing.SPOT_DETAIL_HTTP_SPAN, exception);
            return exception;
        });

        return Map.of(
                "success", true,
                "message", "Spot retrieved successfully",
                "data", spot
        );
    }
}
