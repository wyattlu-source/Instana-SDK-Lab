package com.example.camping.resource;

import com.example.camping.model.Coupon;
import com.example.camping.observability.InstanaTracing;
import com.example.camping.repository.CouponRepository;
import com.instana.sdk.annotation.Span;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api/coupons")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CouponResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CouponResource.class);

    @Inject
    CouponRepository couponRepository;

    @GET
    @Span(type = Span.Type.ENTRY, value = "camping-api-list-coupons", capturedStackFrames = 5)
    public Map<String, Object> listCoupons(
            @Context jakarta.ws.rs.container.ContainerRequestContext requestContext) {
        InstanaTracing.httpEntry("camping-api-list-coupons", "GET", "/api/coupons", 200);

        // 從 JWT 取得 userId
        String userId = (String) requestContext.getProperty("userId");
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        InstanaTracing.entry("camping-api-list-coupons", "tags.user.id", userId);
        LOGGER.info("[COUPON] list all - userId: " + userId);

        // 查詢使用者的所有優惠券
        List<Coupon> coupons = couponRepository.findByUserId(userId);

        // 轉換為回應格式
        List<Map<String, Object>> couponList = new ArrayList<>();
        for (Coupon coupon : coupons) {
            couponList.add(couponToMap(coupon));
        }

        LOGGER.info("[COUPON] found " + couponList.size() + " coupons for userId: " + userId);

        return Map.of(
                "success", true,
                "coupons", couponList,
                "count", couponList.size()
        );
    }

    @GET
    @Path("/available")
    @Span(type = Span.Type.ENTRY, value = "camping-api-available-coupons", capturedStackFrames = 5)
    public Map<String, Object> listAvailableCoupons(
            @Context jakarta.ws.rs.container.ContainerRequestContext requestContext) {
        InstanaTracing.httpEntry("camping-api-available-coupons", "GET", "/api/coupons/available", 200);

        // 從 JWT 取得 userId
        String userId = (String) requestContext.getProperty("userId");
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        InstanaTracing.entry("camping-api-available-coupons", "tags.user.id", userId);
        LOGGER.info("[COUPON] list available - userId: " + userId);

        // 查詢使用者的可用優惠券
        List<Coupon> coupons = couponRepository.findAvailableCoupons(userId);

        // 轉換為回應格式
        List<Map<String, Object>> couponList = new ArrayList<>();
        for (Coupon coupon : coupons) {
            couponList.add(couponToMap(coupon));
        }

        LOGGER.info("[COUPON] found " + couponList.size() + " available coupons for userId: " + userId);

        return Map.of(
                "success", true,
                "coupons", couponList,
                "count", couponList.size()
        );
    }

    @GET
    @Path("/{couponCode}")
    @Span(type = Span.Type.ENTRY, value = "camping-api-get-coupon", capturedStackFrames = 5)
    public Map<String, Object> getCoupon(
            @PathParam("couponCode") String couponCode,
            @Context jakarta.ws.rs.container.ContainerRequestContext requestContext) {
        InstanaTracing.httpEntry("camping-api-get-coupon", "GET", "/api/coupons/" + couponCode, 200);

        // 從 JWT 取得 userId
        String userId = (String) requestContext.getProperty("userId");
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new jakarta.ws.rs.NotAuthorizedException("未授權的請求");
        }

        InstanaTracing.entry("camping-api-get-coupon", "tags.user.id", userId);
        InstanaTracing.entry("camping-api-get-coupon", "tags.coupon.code", couponCode);
        LOGGER.info("[COUPON] get - userId: " + userId + " couponCode: " + couponCode);

        // 查詢優惠券
        Optional<Coupon> couponOpt = couponRepository.findByCouponCode(couponCode);

        if (!couponOpt.isPresent()) {
            LOGGER.warn("[COUPON] not found - couponCode: " + couponCode);
            throw new jakarta.ws.rs.NotFoundException("優惠券不存在");
        }

        Coupon coupon = couponOpt.get();

        // 驗證優惠券是否屬於該使用者
        if (!coupon.getUserId().equals(userId)) {
            LOGGER.warn("[COUPON] access denied - couponCode: " + couponCode + 
                    " userId: " + userId + " owner: " + coupon.getUserId());
            throw new jakarta.ws.rs.ForbiddenException("無權存取此優惠券");
        }

        LOGGER.info("[COUPON] found - couponCode: " + couponCode + " status: " + coupon.getStatus());

        return Map.of(
                "success", true,
                "coupon", couponToMap(coupon)
        );
    }

    private Map<String, Object> couponToMap(Coupon coupon) {
        Map<String, Object> map = new HashMap<>();
        map.put("coupon_id", coupon.getCouponId());
        map.put("coupon_code", coupon.getCouponCode());
        map.put("user_id", coupon.getUserId());
        map.put("spot_id", coupon.getSpotId());
        map.put("spot_name", coupon.getSpotName());
        map.put("discount_amount", coupon.getDiscountAmount());
        map.put("status", coupon.getStatus().getValue());
        map.put("created_at", coupon.getCreatedAt());
        map.put("expires_at", coupon.getExpiresAt());
        
        if (coupon.getUsedAt() != null) {
            map.put("used_at", coupon.getUsedAt());
        }
        
        if (coupon.getOrderId() != null) {
            map.put("order_id", coupon.getOrderId());
        }
        
        return map;
    }
}

// Made with Bob
