package com.example.camping.resource;

import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/favorites")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class FavoriteResource {
    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.FAVORITE_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, Object> trackFavorite(@TagParam("payload") Map<String, Object> payload) {
        InstanaTracing.httpEntry(InstanaTracing.FAVORITE_HTTP_SPAN, "POST", "/api/favorites", 200);
        boolean favorite = Boolean.TRUE.equals(payload.get("is_favorite"));
        String spotId = stringValue(payload.get("spot_id"));
        String spotName = stringValue(payload.get("spot_name"));
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.favorite.enabled", Boolean.toString(favorite));
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.spot.id", spotId);
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.spot.name", spotName);

        if (!favorite) {
            return Map.of(
                    "success", true,
                    "message", "Favorite removed"
            );
        }

        return Map.of(
                "success", true,
                "message", "Favorite tracked",
                "coupon", Map.of(
                        "spot_id", spotId,
                        "spot_name", spotName,
                        "coupon_code", couponCode(spotId),
                        "discount_amount", 100,
                        "message", "Favorite coupon is ready."
                )
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.COUPON_CODE_SPAN, captureArguments = true, captureReturn = true)
    private static String couponCode(@TagParam("spot_id") String spotId) {
        InstanaTracing.method(InstanaTracing.COUPON_CODE_SPAN, FavoriteResource.class.getName(), "couponCode");
        InstanaTracing.intermediate(InstanaTracing.COUPON_CODE_SPAN, "tags.spot.id", spotId);
        String suffix = spotId == null || spotId.length() < 8 ? "CAMPING" : spotId.substring(0, 8);
        return "CAMP-" + suffix.toUpperCase();
    }
}
