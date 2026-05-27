package com.example.consumer.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order Processor Application
 * 訂單處理服務 - 消費 raw_events topic 中的 checkout 事件
 */
public class OrderProcessorApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessorApp.class);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        LOGGER.info("=== Order Processor Service Starting ===");
        LOGGER.info("Version: 1.0.0");
        LOGGER.info("Java Version: {}", System.getProperty("java.version"));
        
        // 註冊優雅關閉 hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown signal received, stopping gracefully...");
            running = false;
            try {
                Thread.sleep(2000); // 等待處理中的訊息完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("Order Processor Service stopped");
        }));

        try {
            // 建立並啟動 Consumer
            OrderConsumer consumer = new OrderConsumer();
            consumer.start();
            
            // 保持主執行緒運行
            while (running) {
                Thread.sleep(1000);
            }
            
            // 關閉 Consumer
            consumer.stop();
            
        } catch (Exception e) {
            LOGGER.error("Fatal error in Order Processor", e);
            System.exit(1);
        }
        
        LOGGER.info("=== Order Processor Service Exited ===");
    }

    public static boolean isRunning() {
        return running;
    }
}

// Made with Bob
