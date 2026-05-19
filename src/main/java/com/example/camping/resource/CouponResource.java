package com.example.camping.resource;

import com.example.camping.model.AuthenticatedUser;
import com.example.camping.model.Coupon;
import com.example.camping.repository.CouponRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/coupons")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CouponResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CouponResource.class);

    @Inject
    CouponRepository couponRepository;

    @Inject
    AuthenticatedUser authenticatedUser;

    @GET
    public Map<String, Object> listCoupons() {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new NotAuthorizedException("未授權的請求");
        }

        LOGGER.warn("[COUPON] list all - userId: " + userId);
        List<Coupon> coupons = couponRepository.findByUserId(userId);

        List<Map<String, Object>> couponList = new ArrayList<>();
        for (Coupon coupon : coupons) {
            couponList.add(couponToMap(coupon));
        }

        LOGGER.warn("[COUPON] found " + couponList.size() + " coupons for userId: " + userId);
        return Map.of("success", true, "coupons", couponList, "count", couponList.size());
    }

    @GET
    @Path("/available")
    public Map<String, Object> listAvailableCoupons() {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new NotAuthorizedException("未授權的請求");
        }

        LOGGER.warn("[COUPON] list available - userId: " + userId);
        List<Coupon> coupons = couponRepository.findAvailableCoupons(userId);

        List<Map<String, Object>> couponList = new ArrayList<>();
        for (Coupon coupon : coupons) {
            couponList.add(couponToMap(coupon));
        }

        LOGGER.warn("[COUPON] found " + couponList.size() + " available coupons for userId: " + userId);
        return Map.of("success", true, "coupons", couponList, "count", couponList.size());
    }

    @GET
    @Path("/{couponCode}")
    public Map<String, Object> getCoupon(@PathParam("couponCode") String couponCode) {
        String userId = authenticatedUser.getUserId();
        if (userId == null || userId.isEmpty()) {
            LOGGER.error("[COUPON] userId not found in request context");
            throw new NotAuthorizedException("未授權的請求");
        }

        LOGGER.warn("[COUPON] get - userId: " + userId + " couponCode: " + couponCode);
        Optional<Coupon> couponOpt = couponRepository.findByCouponCode(couponCode);

        if (!couponOpt.isPresent()) {
            LOGGER.warn("[COUPON] not found - couponCode: " + couponCode);
            throw new NotFoundException("優惠券不存在");
        }

        Coupon coupon = couponOpt.get();
        if (!coupon.getUserId().equals(userId)) {
            LOGGER.warn("[COUPON] access denied - couponCode: " + couponCode + " userId: " + userId);
            throw new ForbiddenException("無權存取此優惠券");
        }

        return Map.of("success", true, "coupon", couponToMap(coupon));
    }

    private Map<String, Object> couponToMap(Coupon coupon) {
        Map<String, Object> map = new HashMap<>();
        map.put("coupon_id", coupon.getCouponId());
        map.put("coupon_code", coupon.getCouponCode());
        map.put("spot_id", coupon.getSpotId());
        map.put("spot_name", coupon.getSpotName());
        map.put("discount_amount", coupon.getDiscountAmount());
        map.put("status", coupon.getStatus().getValue());
        map.put("created_at", coupon.getCreatedAt());
        map.put("expires_at", coupon.getExpiresAt());
        if (coupon.getUsedAt() != null) map.put("used_at", coupon.getUsedAt());
        if (coupon.getOrderId() != null) map.put("order_id", coupon.getOrderId());
        return map;
    }
}
