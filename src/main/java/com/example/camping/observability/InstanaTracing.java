package com.example.camping.observability;

import org.slf4j.Logger;

/** No-op stub — Instana SDK removed in this branch */
public final class InstanaTracing {
    public static final String CHECKOUT_HTTP_SPAN            = "camping-checkout-http";
    public static final String SPOT_LOOKUP_SPAN              = "camping-spot-lookup";
    public static final String ORDER_VALIDATE_SPAN           = "camping-order-validate";
    public static final String AUDIT_RECORD_SPAN             = "camping-audit-record";
    public static final String PRICING_GET_UNIT_SPAN         = "camping-pricing-get-unit";
    public static final String AUTH_VERIFY_TOKEN_SPAN        = "camping-auth-verify-token";
    public static final String AUTH_REGISTER_SPAN            = "camping-auth-register";
    public static final String AUTH_LOGIN_SPAN               = "camping-auth-login";
    public static final String USER_REPO_SAVE_SPAN           = "camping-user-repo-save";
    public static final String USER_REPO_FIND_SPAN           = "camping-user-repo-find";
    public static final String COUPON_REPO_SAVE_SPAN         = "camping-coupon-repo-save";
    public static final String COUPON_REPO_FIND_BY_USER_SPAN = "camping-coupon-repo-find-by-user";
    public static final String COUPON_REPO_FIND_BY_CODE_SPAN = "camping-coupon-repo-find-by-code";
    public static final String COUPON_REPO_USE_SPAN          = "camping-coupon-repo-use";
    public static final String FAVORITE_REPO_SAVE_SPAN       = "camping-favorite-repo-save";
    public static final String FAVORITE_REPO_FIND_SPAN       = "camping-favorite-repo-find";
    public static final String SPOT_DB_SPAN                  = "camping-spot-db";
    public static final String ROOT_HTTP_SPAN                = "camping-api-root";

    private InstanaTracing() {}

    public static void logWarn(Logger logger, String msg) { logger.warn(msg); }
    public static void logInfo(Logger logger, String msg) { logger.info(msg); }
    public static void httpEntry(String span, String method, String path, int status) {}
    public static void method(Object type, String span, String cls, String m) {}
    public static void method(String span, String cls, String m) {}
}
