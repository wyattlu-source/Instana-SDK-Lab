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
    public static final String KAFKA_SEND_SPAN = "camping-kafka-checkout-send";
    public static final String KAFKA_RECORD_SPAN = "camping-kafka-record-build";
    public static final String KAFKA_PRODUCER_INIT_SPAN = "camping-kafka-producer-init";
    public static final String SPOT_LOOKUP_SPAN = "camping-spot-lookup";
    public static final String SPOT_LIST_SPAN = "camping-spot-list";
    public static final String COUPON_CODE_SPAN = "camping-coupon-code";
    public static final String EMAIL_DOMAIN_SPAN = "camping-email-domain";
    public static final String PREFLIGHT_HTTP_SPAN = "camping-api-preflight";

    private InstanaTracing() {
    }

    public static void httpEntry(String spanName, String method, String path, int statusCode) {
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
        SpanSupport.annotate(spanType, spanName, key, safe(value));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
