package com.example.consumer.notification;

import com.example.consumer.notification.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationApp.class);

    public static void main(String[] args) {
        LOGGER.warn("=== Camping Notification Service 啟動 ===");
        LOGGER.warn("版本: 1.0.0 | Java: {}", System.getProperty("java.version"));
        AppConfig.printConfig();

        NotificationConsumer consumer = new NotificationConsumer();

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.warn("[NOTIFICATION] 收到關閉訊號，正在停止...");
            consumer.stop();
        }));

        consumer.start();
    }
}
