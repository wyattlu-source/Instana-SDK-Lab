package com.example.camping.util;

import com.instana.sdk.support.SpanSupport;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class InstanaTracingUtil {
    private static final Logger LOGGER = Logger.getLogger(InstanaTracingUtil.class.getName());

    public static <T> T trace(String methodName, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();

        SpanSupport.annotate("business.method.start", methodName);
        SpanSupport.annotate("business.timestamp.start", String.valueOf(startTime));

        try {
            T result = supplier.get();
            long duration = System.currentTimeMillis() - startTime;

            SpanSupport.annotate("business.method.end", methodName);
            SpanSupport.annotate("business.duration.ms", String.valueOf(duration));
            SpanSupport.annotate("business.status", "success");

            LOGGER.info(String.format("[INSTANA-TRACE] %s - SUCCESS - %dms", methodName, duration));
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            SpanSupport.annotate("business.method.end", methodName);
            SpanSupport.annotate("business.duration.ms", String.valueOf(duration));
            SpanSupport.annotate("business.status", "error");
            SpanSupport.annotate("business.error.type", e.getClass().getSimpleName());
            SpanSupport.annotate("business.error.message", e.getMessage() != null ? e.getMessage() : "");

            LOGGER.severe(String.format("[INSTANA-TRACE] %s - ERROR - %dms - %s",
                    methodName, duration, e.getMessage()));
            throw e;
        }
    }

    public static void traceVoid(String methodName, Runnable runnable) {
        trace(methodName, () -> {
            runnable.run();
            return null;
        });
    }

    public static void addBusinessTag(String key, Object value) {
        if (value != null) {
            SpanSupport.annotate("business." + key, String.valueOf(value));
        }
    }

    public static void markStep(String stepName, String description) {
        SpanSupport.annotate("business.step", stepName);
        SpanSupport.annotate("business.step.description", description);
        LOGGER.info(String.format("[INSTANA-STEP] %s: %s", stepName, description));
    }

    public static void logBusinessEvent(String eventType, String eventData) {
        SpanSupport.annotate("business.event.type", eventType);
        SpanSupport.annotate("business.event.data", eventData);
        SpanSupport.annotate("business.event.timestamp", String.valueOf(System.currentTimeMillis()));
        LOGGER.info(String.format("[INSTANA-EVENT] %s: %s", eventType, eventData));
    }
}
