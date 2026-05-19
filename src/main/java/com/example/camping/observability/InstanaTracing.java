package com.example.camping.observability;

import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;

public final class InstanaTracing {

    public static final String ROOT_HTTP_SPAN = "camping-api-root";
    public static final String HEALTH_HTTP_SPAN = "camping-api-health";
    public static final String SPOTS_HTTP_SPAN = "camping-api-list-spots";
    public static final String SPOT_DETAIL_HTTP_SPAN = "camping-api-get-spot";
    public static final String NEWSLETTER_HTTP_SPAN = "camping-api-newsletter";
    public static final String CHECKOUT_HTTP_SPAN = "camping-api-checkout";
    public static final String CHECKOUT_ASYNC_JOB_SPAN = "camping-checkout-async-job";
    public static final String FAVORITE_HTTP_SPAN = "camping-api-favorite";
    public static final String FAVORITE_LIST_HTTP_SPAN = "camping-api-list-favorites";
    public static final String KAFKA_SEND_SPAN = "camping-kafka-checkout-send";
    public static final String KAFKA_RECORD_SPAN = "camping-kafka-record-build";
    public static final String KAFKA_PRODUCER_INIT_SPAN = "camping-kafka-producer-init";
    public static final String SPOT_LOOKUP_SPAN = "camping-spot-lookup";
    public static final String SPOT_LIST_SPAN = "camping-spot-list";
    public static final String COUPON_CODE_SPAN = "camping-coupon-code";
    public static final String EMAIL_DOMAIN_SPAN = "camping-email-domain";
    public static final String PREFLIGHT_HTTP_SPAN = "camping-api-preflight";
    public static final String HTTP_SPOT_SERVICE_SPAN = "camping-http-spot-service-exit";

    // Shared utility spans
    public static final String AUDIT_SPAN = "camping-audit-record";
    public static final String ORDER_VALIDATE_SPAN = "camping-order-validate";
    public static final String PRICING_SPAN = "camping-pricing-calculate";

    // Auth spans
    public static final String AUTH_REGISTER_HTTP_SPAN = "camping-auth-register";
    public static final String AUTH_LOGIN_HTTP_SPAN = "camping-auth-login";
    public static final String AUTH_LOGOUT_HTTP_SPAN = "camping-auth-logout";
    public static final String AUTH_REGISTER_SERVICE_SPAN = "camping-auth-service-register";
    public static final String AUTH_LOGIN_SERVICE_SPAN = "camping-auth-service-login";
    public static final String AUTH_VERIFY_TOKEN_SPAN = "camping-auth-verify-token";
    public static final String USER_REPO_SAVE_SPAN = "camping-user-repo-save";
    public static final String USER_REPO_FIND_SPAN = "camping-user-repo-findByEmail";
    public static final String USER_REPO_EXISTS_SPAN = "camping-user-repo-existsByEmail";

    // Favorite repository spans
    public static final String FAVORITE_REPO_SAVE_SPAN = "camping-favorite-repo-save";
    public static final String FAVORITE_REPO_FIND_BY_USER_SPAN = "camping-favorite-repo-findByUserId";
    public static final String FAVORITE_REPO_FIND_ACTIVE_SPAN = "camping-favorite-repo-findActiveByUserId";
    public static final String FAVORITE_REPO_EXISTS_SPAN = "camping-favorite-repo-existsByUserAndSpot";
    public static final String FAVORITE_REPO_CANCEL_SPAN = "camping-favorite-repo-cancelFavorite";
    public static final String FAVORITE_REPO_REACTIVATE_SPAN = "camping-favorite-repo-reactivateFavorite";

    // Coupon repository spans
    public static final String COUPON_REPO_SAVE_SPAN = "camping-coupon-repo-save";
    public static final String COUPON_REPO_FIND_BY_USER_SPAN = "camping-coupon-repo-findByUserId";
    public static final String COUPON_REPO_FIND_AVAILABLE_SPAN = "camping-coupon-repo-findAvailableCoupons";
    public static final String COUPON_REPO_FIND_BY_CODE_SPAN = "camping-coupon-repo-findByCouponCode";
    public static final String COUPON_REPO_USE_SPAN = "camping-coupon-repo-useCoupon";
    public static final String COUPON_REPO_EXPIRE_SPAN = "camping-coupon-repo-expireOldCoupons";

    // Thread-local index so multiple log calls in one span get distinct tag keys
    private static final ThreadLocal<int[]> LOG_IDX = ThreadLocal.withInitial(() -> new int[]{0});

    private InstanaTracing() {
    }

    // ── Log helpers: write to SLF4J + annotate current Instana span ──

    public static void logInfo(org.slf4j.Logger logger, String message) {
        if (logger != null) logger.info(message);
        annotateLog("INFO", message);
    }

    public static void logWarn(org.slf4j.Logger logger, String message) {
        if (logger != null) logger.warn(message);
        annotateLog("WARN", message);
    }

    public static void logError(org.slf4j.Logger logger, String message) {
        if (logger != null) logger.error(message);
        annotateLog("ERROR", message);
    }

    public static void logError(org.slf4j.Logger logger, String message, Throwable t) {
        if (logger != null) logger.error(message, t);
        annotateLog("ERROR", message);
        SpanSupport.annotate("log.error.type", t.getClass().getSimpleName());
        if (t.getMessage() != null) SpanSupport.annotate("log.error.detail", safe(t.getMessage()));
    }

    private static void annotateLog(String level, String message) {
        int idx = LOG_IDX.get()[0]++;
        SpanSupport.annotate("log." + idx + ".level", level);
        SpanSupport.annotate("log." + idx + ".msg", safe(message));
    }

    public static void httpEntry(String spanName, String method, String path, int statusCode) {
        LOG_IDX.get()[0] = 0; // reset log index for new span
        annotate(Span.Type.ENTRY, spanName, "tags.http.method", method);
        annotate(Span.Type.ENTRY, spanName, "tags.http.url", "service://camping-api" + path);
        annotate(Span.Type.ENTRY, spanName, "tags.http.status_code", Integer.toString(statusCode));
        annotate(Span.Type.ENTRY, spanName, "tags.service", "camping-api");
        annotate(Span.Type.ENTRY, spanName, "tags.endpoint", method + " " + path);
    }

    public static void batchJob(String spanName, String jobName) {
        annotate(Span.Type.ENTRY, spanName, "tags.batch.job", jobName);
        annotate(Span.Type.ENTRY, spanName, "tags.service", "camping-api");
        annotate(Span.Type.ENTRY, spanName, "tags.endpoint", jobName);
    }

    public static void kafkaExit(String topic, String key, String eventType) {
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.message_bus.destination", topic);
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.message_bus.operation", "send");
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.kafka.topic", topic);
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.kafka.key", safe(key));
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.event.type", safe(eventType));
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.service", "camping-api");
        annotate(Span.Type.EXIT, KAFKA_SEND_SPAN, "tags.endpoint", "kafka send " + topic);
    }

    public static void intermediate(String spanName, String key, String value) {
        annotate(Span.Type.INTERMEDIATE, spanName, key, value);
    }

    public static void method(String spanName, String className, String methodName) {
        method(Span.Type.INTERMEDIATE, spanName, className, methodName);
    }

    public static void method(Span.Type spanType, String spanName, String className, String methodName) {
        annotate(spanType, spanName, "tags.java.class", className);
        annotate(spanType, spanName, "tags.java.method", methodName);
    }

    public static void entry(String spanName, String key, String value) {
        annotate(Span.Type.ENTRY, spanName, key, value);
    }

    public static void error(Span.Type spanType, String spanName, Throwable throwable) {
        annotate(spanType, spanName, "tags.error", "true");
        annotate(spanType, spanName, "tags.error.message", throwable.getMessage());
        annotate(spanType, spanName, "tags.error.type", throwable.getClass().getName());
    }

    private static void annotate(Span.Type spanType, String spanName, String key, String value) {
        SpanSupport.annotate(key, safe(value));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
