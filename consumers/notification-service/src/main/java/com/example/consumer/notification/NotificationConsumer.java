package com.example.consumer.notification;

import com.example.consumer.notification.config.AppConfig;
import com.example.consumer.notification.model.RawEvent;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class NotificationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConsumer.class);

    private final KafkaConsumer<String, GenericRecord> consumer;
    private final EmailService emailService;
    private volatile boolean running = false;

    public NotificationConsumer() {
        this.emailService = new EmailService();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.getKafkaBootstrapServer());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, AppConfig.getConsumerGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, AppConfig.getSchemaRegistryUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, AppConfig.getMaxPollRecords());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

        this.consumer = new KafkaConsumer<>(props);
        LOGGER.warn("[NOTIFICATION] Consumer 初始化完成，準備訂閱 {}", AppConfig.getSourceTopic());
    }

    public void start() {
        running = true;
        consumer.subscribe(Collections.singletonList(AppConfig.getSourceTopic()));
        LOGGER.warn("[NOTIFICATION] 已訂閱 topic: {}", AppConfig.getSourceTopic());
        consumeLoop();
    }

    @Span(type = Span.Type.ENTRY, value = "notification-consume-loop")
    private void consumeLoop() {
        long processed = 0, skipped = 0, errors = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(AppConfig.getPollTimeoutMs()));
                if (records.isEmpty()) continue;

                LOGGER.warn("[NOTIFICATION] 收到 {} 筆訊息", records.count());
                for (ConsumerRecord<String, GenericRecord> record : records) {
                    try {
                        boolean sent = processRecord(record);
                        if (sent) processed++; else skipped++;
                    } catch (Exception e) {
                        errors++;
                        LOGGER.error("[NOTIFICATION] 處理失敗 offset={}: {}", record.offset(), e.getMessage(), e);
                    }
                }
                consumer.commitSync();

                if ((processed + skipped) % 50 == 0 && (processed + skipped) > 0) {
                    LOGGER.warn("[NOTIFICATION] 統計: 已寄={} 跳過={} 失敗={}", processed, skipped, errors);
                }
            } catch (Exception e) {
                LOGGER.error("[NOTIFICATION] consume loop 錯誤: {}", e.getMessage(), e);
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "notification-process-record")
    private boolean processRecord(ConsumerRecord<String, GenericRecord> record) {
        // 繼承 Instana trace context
        Map<String, String> traceHeaders = new HashMap<>();
        for (Header h : record.headers()) {
            if (h.key().startsWith("X-Instana-")) {
                traceHeaders.put(h.key(), new String(h.value()));
            }
        }
        if (!traceHeaders.isEmpty()) {
            SpanSupport.annotate("trace.continued", "true");
        }

        SpanSupport.annotate("kafka.topic", record.topic());
        SpanSupport.annotate("kafka.offset", String.valueOf(record.offset()));

        RawEvent event = deserialize(record.value());

        // 只處理 ORDER_CREATED 事件
        if (!"ORDER_CREATED".equals(event.getEventType())) {
            LOGGER.warn("[NOTIFICATION] 跳過非 ORDER_CREATED 事件: {}", event.getEventType());
            return false;
        }

        if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            LOGGER.warn("[NOTIFICATION] 跳過缺少 email 的事件: orderId={}", event.getOrderId());
            return false;
        }

        LOGGER.warn("[NOTIFICATION] 處理訂單確認通知: orderId={} email={} product={}",
                event.getOrderId(), event.getUserEmail(), event.getProductName());

        emailService.sendOrderConfirmation(event);
        return true;
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "notification-deserialize")
    private RawEvent deserialize(GenericRecord r) {
        RawEvent e = new RawEvent();
        e.setEventType(str(r, "event_type"));
        e.setEventId(str(r, "event_id"));
        e.setSessionId(str(r, "session_id"));
        e.setUserEmail(str(r, "user_email"));
        e.setUserName(str(r, "user_name"));
        e.setOrderId(str(r, "order_id"));
        e.setProductId(str(r, "product_id"));
        e.setProductName(str(r, "product_name"));
        e.setAmount(intVal(r, "amount"));
        e.setOrderStatus(str(r, "order_status"));
        e.setTs(longVal(r, "ts"));
        return e;
    }

    private String  str(GenericRecord r, String f)  { Object v = r.get(f); return v != null ? v.toString() : null; }
    private Integer intVal(GenericRecord r, String f){ Object v = r.get(f); return v instanceof Integer ? (Integer)v : null; }
    private Long    longVal(GenericRecord r, String f){ Object v = r.get(f); return v instanceof Long ? (Long)v : null; }

    public void stop() {
        LOGGER.warn("[NOTIFICATION] 正在關閉...");
        running = false;
        if (consumer != null) {
            try { consumer.close(Duration.ofSeconds(10)); } catch (Exception ignore) {}
        }
    }
}
