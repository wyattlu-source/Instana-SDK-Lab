package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.Coupon;
import com.example.camping.model.CouponStatus;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CouponRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponRepository.class);
    private static final String COLLECTION = "coupons";

    @Inject
    MongoConfig mongoConfig;

    @PostConstruct
    public void init() {
        if (!mongoConfig.isAvailable()) return;
        try {
            // 唯一索引: couponCode
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("couponCode", 1),
                            new IndexOptions().unique(true)
                    );

            // 使用者索引: {userId, status, expiresAt}
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("userId", 1)
                                    .append("status", 1)
                                    .append("expiresAt", -1)
                    );

            // 過期索引: {status, expiresAt}
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("status", 1)
                                    .append("expiresAt", 1)
                    );

            // 訂單索引: orderId
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(new Document("orderId", 1));
        } catch (Exception e) {
            LOGGER.warn("Index creation failed: " + e.getMessage());
        }
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_SAVE_SPAN, capturedStackFrames = 5)
    public void save(@TagParam("coupon") Coupon coupon) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_SAVE_SPAN,
                CouponRepository.class.getName(), "save");
        annotateMongoExit(InstanaTracing.COUPON_REPO_SAVE_SPAN, "insertOne");

        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            LOGGER.error("[MONGODB] save failed - database unavailable, couponId: " +
                    coupon.getCouponId(), ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.COUPON_REPO_SAVE_SPAN, ex);
            throw ex;
        }

        // 生成 couponId 如果不存在
        if (coupon.getCouponId() == null || coupon.getCouponId().isEmpty()) {
            coupon.setCouponId(UUID.randomUUID().toString());
        }

        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .insertOne(toDocument(coupon));

        LOGGER.warn("[MONGODB] Coupon saved - couponId: " + coupon.getCouponId() +
                ", couponCode: " + coupon.getCouponCode() + ", userId: " + coupon.getUserId());
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_FIND_BY_USER_SPAN, capturedStackFrames = 5)
    public List<Coupon> findByUserId(@TagParam("userId") String userId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_FIND_BY_USER_SPAN,
                CouponRepository.class.getName(), "findByUserId");
        annotateMongoExit(InstanaTracing.COUPON_REPO_FIND_BY_USER_SPAN, "find");

        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findByUserId failed - database unavailable, userId: " + userId);
            return new ArrayList<>();
        }

        List<Coupon> coupons = new ArrayList<>();
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(Filters.eq("userId", userId))
                .sort(new Document("createdAt", -1))
                .into(new ArrayList<>())
                .forEach(doc -> coupons.add(toCoupon(doc)));

        LOGGER.warn("[MONGODB] Found " + coupons.size() + " coupons for userId: " + userId);
        return coupons;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_FIND_AVAILABLE_SPAN, capturedStackFrames = 5)
    public List<Coupon> findAvailableCoupons(@TagParam("userId") String userId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_FIND_AVAILABLE_SPAN,
                CouponRepository.class.getName(), "findAvailableCoupons");
        annotateMongoExit(InstanaTracing.COUPON_REPO_FIND_AVAILABLE_SPAN, "find");

        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findAvailableCoupons failed - database unavailable, userId: " + userId);
            return new ArrayList<>();
        }

        long now = System.currentTimeMillis();
        List<Coupon> coupons = new ArrayList<>();
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("status", CouponStatus.UNUSED.getValue()),
                Filters.gt("expiresAt", now)
        );

        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(filter)
                .sort(new Document("expiresAt", 1))
                .into(new ArrayList<>())
                .forEach(doc -> coupons.add(toCoupon(doc)));

        LOGGER.warn("[MONGODB] Found " + coupons.size() + " available coupons for userId: " + userId);
        return coupons;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_FIND_BY_CODE_SPAN, capturedStackFrames = 5)
    public Optional<Coupon> findByCouponCode(@TagParam("couponCode") String couponCode) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_FIND_BY_CODE_SPAN,
                CouponRepository.class.getName(), "findByCouponCode");
        annotateMongoExit(InstanaTracing.COUPON_REPO_FIND_BY_CODE_SPAN, "find");

        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findByCouponCode failed - database unavailable, couponCode: " + couponCode);
            return Optional.empty();
        }

        Document doc = mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(Filters.eq("couponCode", couponCode))
                .first();

        return Optional.ofNullable(doc).map(this::toCoupon);
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_USE_SPAN, capturedStackFrames = 5)
    public boolean useCoupon(@TagParam("couponCode") String couponCode, @TagParam("orderId") String orderId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_USE_SPAN,
                CouponRepository.class.getName(), "useCoupon");
        annotateMongoExit(InstanaTracing.COUPON_REPO_USE_SPAN, "updateOne");

        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            LOGGER.error("[MONGODB] useCoupon failed - database unavailable, couponCode: " +
                    couponCode, ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.COUPON_REPO_USE_SPAN, ex);
            throw ex;
        }

        long now = System.currentTimeMillis();
        
        // 原子更新: 只有當優惠券狀態為 UNUSED 且未過期時才更新
        Bson filter = Filters.and(
                Filters.eq("couponCode", couponCode),
                Filters.eq("status", CouponStatus.UNUSED.getValue()),
                Filters.gt("expiresAt", now)
        );

        Bson update = Updates.combine(
                Updates.set("status", CouponStatus.USED.getValue()),
                Updates.set("usedAt", now),
                Updates.set("orderId", orderId)
        );

        long modifiedCount = mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .updateOne(filter, update)
                .getModifiedCount();

        boolean success = modifiedCount > 0;
        if (success) {
            LOGGER.warn("[MONGODB] Coupon used - couponCode: " + couponCode + ", orderId: " + orderId);
        } else {
            LOGGER.warn("[MONGODB] Coupon use failed - couponCode: " + couponCode + 
                    " (may be already used, expired, or not found)");
        }

        return success;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.COUPON_REPO_EXPIRE_SPAN, capturedStackFrames = 5)
    public int expireOldCoupons() {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.COUPON_REPO_EXPIRE_SPAN,
                CouponRepository.class.getName(), "expireOldCoupons");
        annotateMongoExit(InstanaTracing.COUPON_REPO_EXPIRE_SPAN, "updateMany");

        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] expireOldCoupons failed - database unavailable");
            return 0;
        }

        long now = System.currentTimeMillis();

        // 批次更新: 將過期的 UNUSED 優惠券標記為 EXPIRED
        Bson filter = Filters.and(
                Filters.eq("status", CouponStatus.UNUSED.getValue()),
                Filters.lte("expiresAt", now)
        );

        Bson update = Updates.set("status", CouponStatus.EXPIRED.getValue());

        long modifiedCount = mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .updateMany(filter, update)
                .getModifiedCount();

        LOGGER.warn("[MONGODB] Expired " + modifiedCount + " old coupons");
        return (int) modifiedCount;
    }

    private void annotateMongoExit(String spanName, String operation) {
        com.instana.sdk.support.SpanSupport.annotate("tags.db.type", "mongodb");
        com.instana.sdk.support.SpanSupport.annotate("tags.db.collection", COLLECTION);
        com.instana.sdk.support.SpanSupport.annotate("tags.db.operation", operation);
        com.instana.sdk.support.SpanSupport.annotate("tags.service", "camping-api");
    }

    private Coupon toCoupon(Document doc) {
        String statusValue = doc.getString("status");
        CouponStatus status = statusValue != null ? 
                CouponStatus.fromValue(statusValue) : CouponStatus.UNUSED;

        return new Coupon(
                doc.getString("couponId"),
                doc.getString("couponCode"),
                doc.getString("userId"),
                doc.getString("spotId"),
                doc.getString("spotName"),
                doc.getInteger("discountAmount", 0),
                status,
                doc.getLong("createdAt"),
                doc.getLong("expiresAt"),
                doc.getLong("usedAt"),
                doc.getString("orderId")
        );
    }

    private Document toDocument(Coupon coupon) {
        Document doc = new Document()
                .append("couponId", coupon.getCouponId())
                .append("couponCode", coupon.getCouponCode())
                .append("userId", coupon.getUserId())
                .append("spotId", coupon.getSpotId())
                .append("spotName", coupon.getSpotName())
                .append("discountAmount", coupon.getDiscountAmount())
                .append("status", coupon.getStatus().getValue())
                .append("createdAt", coupon.getCreatedAt())
                .append("expiresAt", coupon.getExpiresAt());

        if (coupon.getUsedAt() != null) {
            doc.append("usedAt", coupon.getUsedAt());
        }

        if (coupon.getOrderId() != null) {
            doc.append("orderId", coupon.getOrderId());
        }

        return doc;
    }
}

// Made with Bob
