package com.example.camping.resource;

import com.example.camping.model.Coupon;
import com.example.camping.model.CouponStatus;
import com.example.camping.model.Favorite;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.repository.CouponRepository;
import com.example.camping.repository.FavoriteRepository;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.example.camping.model.AuthenticatedUser;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/favorites")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class FavoriteResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteResource.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    FavoriteRepository favoriteRepository;

    @Inject
    CouponRepository couponRepository;

    @Inject
    AuthenticatedUser authenticatedUser;

    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.FAVORITE_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, Object> trackFavorite(
            @TagParam("payload") Map<String, Object> payload) {
        InstanaTracing.httpEntry(InstanaTracing.FAVORITE_HTTP_SPAN, "POST", "/api/favorites", 200);
        
        // 從 JWT 取得 userId
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[FAVORITE] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        boolean favorite = Boolean.TRUE.equals(payload.get("is_favorite"));
        String spotId = stringValue(payload.get("spot_id"));
        String spotName = stringValue(payload.get("spot_name"));
        
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.user.id", userId);
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.favorite.enabled", Boolean.toString(favorite));
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.spot.id", spotId);
        InstanaTracing.entry(InstanaTracing.FAVORITE_HTTP_SPAN, "tags.spot.name", spotName);

        LOGGER.warn("[FAVORITE] track - userId: " + userId + " spot_id: " + spotId +
                " spot_name: " + spotName + " is_favorite: " + favorite);

        // 檢查是否已收藏
        boolean exists = favoriteRepository.existsByUserAndSpot(userId, spotId);

        if (!favorite) {
            // 取消收藏
            if (exists) {
                favoriteRepository.cancelFavorite(userId, spotId);
                LOGGER.warn("[FAVORITE] canceled - userId: " + userId + " spot_id: " + spotId);
            } else {
                LOGGER.warn("[FAVORITE] not found to cancel - userId: " + userId + " spot_id: " + spotId);
            }
            return Map.of(
                    "success", true,
                    "message", "Favorite removed"
            );
        }

        // 新增或重新啟用收藏
        if (exists) {
            // 已存在,重新啟用
            favoriteRepository.reactivateFavorite(userId, spotId);
            LOGGER.warn("[FAVORITE] reactivated - userId: " + userId + " spot_id: " + spotId);
        } else {
            // 建立新收藏
            Favorite newFavorite = new Favorite(
                    null, // favoriteId 會在 repository 中生成
                    userId,
                    spotId,
                    spotName,
                    System.currentTimeMillis(),
                    true,
                    null
            );
            favoriteRepository.save(newFavorite);
            LOGGER.warn("[FAVORITE] created - userId: " + userId + " spot_id: " + spotId);
        }

        // 建立優惠券並儲存到 MongoDB
        String couponCode = generateUniqueCouponCode(spotId);
        long now = System.currentTimeMillis();
        long expiresAt = now + 30L * 24 * 60 * 60 * 1000; // 30 天後過期
        
        Coupon coupon = new Coupon(
                null, // couponId 會在 repository 中生成
                couponCode,
                userId,
                spotId,
                spotName,
                100, // 折扣金額 100 元
                CouponStatus.UNUSED,
                now,
                expiresAt,
                null,
                null
        );
        
        try {
            couponRepository.save(coupon);
            LOGGER.warn("[FAVORITE] coupon created - userId: " + userId +
                    " spot_id: " + spotId + " coupon_code: " + couponCode);
        } catch (Exception e) {
            LOGGER.error("[FAVORITE] failed to create coupon - userId: " + userId +
                    " spot_id: " + spotId, e);
            // 即使優惠券建立失敗,仍然返回成功(收藏已建立)
        }
        
        return Map.of(
                "success", true,
                "message", "Favorite tracked",
                "coupon", Map.of(
                        "spot_id", spotId,
                        "spot_name", spotName,
                        "coupon_code", couponCode,
                        "discount_amount", 100,
                        "expires_at", expiresAt,
                        "message", "收藏成功!已發放 100 元優惠券,有效期 30 天"
                )
        );
    }

    @GET
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.FAVORITE_LIST_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, Object> listFavorites() {
        InstanaTracing.httpEntry(InstanaTracing.FAVORITE_LIST_HTTP_SPAN, "GET", "/api/favorites", 200);
        
        // 從 JWT 取得 userId
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[FAVORITE] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        InstanaTracing.entry(InstanaTracing.FAVORITE_LIST_HTTP_SPAN, "tags.user.id", userId);
        LOGGER.warn("[FAVORITE] list - userId: " + userId);

        // 查詢使用者的有效收藏
        List<Favorite> favorites = favoriteRepository.findActiveByUserId(userId);
        
        // 轉換為回應格式
        List<Map<String, Object>> favoriteList = new ArrayList<>();
        for (Favorite fav : favorites) {
            Map<String, Object> favMap = new HashMap<>();
            favMap.put("favorite_id", fav.getFavoriteId());
            favMap.put("spot_id", fav.getSpotId());
            favMap.put("spot_name", fav.getSpotName());
            favMap.put("created_at", fav.getCreatedAt());
            favMap.put("active", fav.isActive());
            favoriteList.add(favMap);
        }

        LOGGER.warn("[FAVORITE] found " + favoriteList.size() + " favorites for userId: " + userId);
        
        return Map.of(
                "success", true,
                "favorites", favoriteList,
                "count", favoriteList.size()
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.COUPON_CODE_SPAN, captureArguments = true, captureReturn = true)
    private String generateUniqueCouponCode(@TagParam("spot_id") String spotId) {
        InstanaTracing.method(InstanaTracing.COUPON_CODE_SPAN, FavoriteResource.class.getName(), "generateUniqueCouponCode");
        InstanaTracing.intermediate(InstanaTracing.COUPON_CODE_SPAN, "tags.spot.id", spotId);
        
        // 格式: CAMP-{spotId前8碼}-{隨機4碼}
        String spotPrefix = spotId == null || spotId.length() < 8 ?
                "CAMPING0" : spotId.substring(0, 8);
        
        // 生成唯一優惠券代碼,最多嘗試 10 次
        for (int attempt = 0; attempt < 10; attempt++) {
            String randomSuffix = generateRandomCode(4);
            String couponCode = "CAMP-" + spotPrefix.toUpperCase() + "-" + randomSuffix;
            
            // 檢查是否已存在
            if (!couponRepository.findByCouponCode(couponCode).isPresent()) {
                return couponCode;
            }
        }
        
        // 如果 10 次都失敗,使用時間戳確保唯一性
        String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
        return "CAMP-" + spotPrefix.toUpperCase() + "-" + timestamp;
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return code.toString();
    }
}
