package com.example.consumer.order;

import com.example.consumer.order.model.RawEvent;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Order Processor
 * 處理訂單相關的業務邏輯
 */
public class OrderProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessor.class);
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private long processedCount = 0;
    private long totalAmount = 0;

    @Span(type = Span.Type.INTERMEDIATE, value = "process-order")
    public void process(RawEvent event) {
        SpanSupport.annotate("order.id", event.getOrderId());
        SpanSupport.annotate("order.user", event.getUserEmail());
        SpanSupport.annotate("order.product", event.getProductName());
        SpanSupport.annotate("order.amount", String.valueOf(event.getAmount()));
        
        try {
            // 1. 驗證訂單資料
            validateOrder(event);
            
            // 2. 執行後處理邏輯
            performPostProcessing(event);
            
            // 3. 更新統計
            updateStatistics(event);
            
            // 4. 記錄成功
            logSuccess(event);
            
            processedCount++;
            
        } catch (Exception e) {
            LOGGER.error("Failed to process order {}: {}", event.getOrderId(), e.getMessage(), e);
            SpanSupport.annotate("error", "true");
            SpanSupport.annotate("error.message", e.getMessage());
            throw new RuntimeException("Order processing failed", e);
        }
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "validate-order")
    private void validateOrder(RawEvent event) {
        if (event.getOrderId() == null || event.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        
        if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }
        
        if (event.getAmount() == null || event.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid order amount");
        }
        
        if (!"confirmed".equals(event.getOrderStatus())) {
            LOGGER.warn("Order {} has non-confirmed status: {}", 
                event.getOrderId(), event.getOrderStatus());
        }
        
        LOGGER.debug("Order validation passed: {}", event.getOrderId());
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "post-processing")
    private void performPostProcessing(RawEvent event) {
        // 這裡可以執行各種後處理邏輯:
        
        // 1. 發送到下游 topic (orders_processed)
        sendToDownstreamTopic(event);
        
        // 2. 更新庫存系統
        updateInventory(event);
        
        // 3. 觸發通知
        triggerNotification(event);
        
        // 4. 記錄到資料倉儲
        logToDataWarehouse(event);
        
        LOGGER.info("Post-processing completed for order: {}", event.getOrderId());
    }

    @Span(type = Span.Type.EXIT, value = "send-to-downstream")
    private void sendToDownstreamTopic(RawEvent event) {
        // 實際實作會發送到 Kafka orders_processed topic
        // 這裡先用 log 模擬
        LOGGER.info("→ Sending to orders_processed topic: order_id={}", event.getOrderId());
        SpanSupport.annotate("kafka.topic", "orders_processed");
        SpanSupport.annotate("kafka.key", event.getOrderId());
        
        // TODO: 實作 Kafka Producer 發送邏輯
        // producer.send(new ProducerRecord<>("orders_processed", event.getOrderId(), event));
    }

    @Span(type = Span.Type.EXIT, value = "update-inventory")
    private void updateInventory(RawEvent event) {
        // 模擬更新庫存系統
        LOGGER.info("→ Updating inventory: product_id={}, order_id={}", 
            event.getProductId(), event.getOrderId());
        SpanSupport.annotate("inventory.product_id", event.getProductId());
        
        // TODO: 實作庫存系統 API 呼叫
        // inventoryClient.decreaseStock(event.getProductId(), 1);
    }

    @Span(type = Span.Type.EXIT, value = "trigger-notification")
    private void triggerNotification(RawEvent event) {
        // 模擬觸發通知
        LOGGER.info("→ Triggering notification: user={}, order_id={}", 
            event.getUserEmail(), event.getOrderId());
        SpanSupport.annotate("notification.type", "order_confirmation");
        SpanSupport.annotate("notification.recipient", event.getUserEmail());
        
        // TODO: 實作通知服務呼叫
        // notificationService.sendOrderConfirmation(event);
    }

    @Span(type = Span.Type.EXIT, value = "log-to-warehouse")
    private void logToDataWarehouse(RawEvent event) {
        // 模擬記錄到資料倉儲
        LOGGER.info("→ Logging to data warehouse: order_id={}, amount={}", 
            event.getOrderId(), event.getAmount());
        SpanSupport.annotate("warehouse.table", "orders");
        
        // TODO: 實作資料倉儲寫入
        // warehouseClient.insert("orders", event);
    }

    private void updateStatistics(RawEvent event) {
        if (event.getAmount() != null) {
            totalAmount += event.getAmount();
        }
        
        if (processedCount % 10 == 0) {
            LOGGER.info("Statistics: processed={}, total_amount={}", 
                processedCount, totalAmount);
        }
    }

    private void logSuccess(RawEvent event) {
        String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTs()));
        
        LOGGER.info("✓ Order processed successfully:");
        LOGGER.info("  Order ID: {}", event.getOrderId());
        LOGGER.info("  User: {}", event.getUserEmail());
        LOGGER.info("  Product: {}", event.getProductName());
        LOGGER.info("  Amount: ${}", event.getAmount());
        LOGGER.info("  Status: {}", event.getOrderStatus());
        LOGGER.info("  Timestamp: {}", timestamp);
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }
}

// Made with Bob
