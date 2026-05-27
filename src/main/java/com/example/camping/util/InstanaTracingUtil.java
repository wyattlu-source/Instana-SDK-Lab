package com.example.camping.util;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class InstanaTracingUtil {
    private static final Logger LOGGER = Logger.getLogger(InstanaTracingUtil.class.getName());
    
    // SDK 啟用開關 - 透過環境變數 INSTANA_SDK_ENABLED 控制 (預設: true)
    private static final boolean SDK_ENABLED = Boolean.parseBoolean(
        System.getenv().getOrDefault("INSTANA_SDK_ENABLED", "true")
    );

    public static <T> T trace(String methodName, Supplier<T> supplier) {
        if (!SDK_ENABLED) {
            // SDK 停用時,直接執行業務邏輯,不進行追蹤
            return supplier.get();
        }
        
        long startTime = System.currentTimeMillis();

        try {
            T result = supplier.get();
            long duration = System.currentTimeMillis() - startTime;

            LOGGER.info(String.format("[INSTANA-TRACE] %s - SUCCESS - %dms", methodName, duration));
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

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
        if (!SDK_ENABLED) return;
        if (value != null) {
        }
    }

    public static void markStep(String stepName, String description) {
        if (!SDK_ENABLED) return;
        LOGGER.info(String.format("[INSTANA-STEP] %s: %s", stepName, description));
    }

    public static void logBusinessEvent(String eventType, String eventData) {
        if (!SDK_ENABLED) return;
        LOGGER.info(String.format("[INSTANA-EVENT] %s: %s", eventType, eventData));
    }
}
