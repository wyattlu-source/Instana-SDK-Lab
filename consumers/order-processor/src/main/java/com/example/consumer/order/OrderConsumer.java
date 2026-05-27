package com.example.consumer.order;

import com.example.consumer.order.config.AppConfig;
import com.example.consumer.order.model.RawEvent;
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
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Order Consumer
 * 消費 raw_events topic 並處理 checkout 事件
 */
public class OrderConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumer.class);
    private KafkaConsumer<String, GenericRecord> consumer;
    private OrderProcessor processor;
    private volatile boolean running = false;

    public OrderConsumer() {
        this.processor = new OrderProcessor();
        initializeConsumer();
    }

    private void initializeConsumer() {
        Properties props = new Properties();
        
        // Kafka Consumer 配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.getKafkaBootstrapServer());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, AppConfig.getConsumerGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        
        // Schema Registry 配置
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, AppConfig.getSchemaRegistryUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");
        
        // Consumer 行為配置
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, AppConfig.getMaxPollRecords());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "10000");
        
        this.consumer = new KafkaConsumer<>(props);
        
        LOGGER.info("Kafka Consumer initialized");
        LOGGER.info("  Bootstrap Servers: {}", AppConfig.getKafkaBootstrapServer());
        LOGGER.info("  Group ID: {}", AppConfig.getConsumerGroupId());
        LOGGER.info("  Topic: {}", AppConfig.getSourceTopic());
    }

    public void start() {
        running = true;
        
        // 訂閱 topic
        consumer.subscribe(Collections.singletonList(AppConfig.getSourceTopic()));
        LOGGER.info("Subscribed to topic: {}", AppConfig.getSourceTopic());
        
        // 開始消費循環
        consumeLoop();
    }

    @Span(type = Span.Type.ENTRY, value = "kafka-consume-loop")
    private void consumeLoop() {
        LOGGER.info("Starting consume loop...");
        
        long totalProcessed = 0;
        long totalErrors = 0;
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(
                    Duration.ofMillis(AppConfig.getPollTimeoutMs())
                );
                
                if (records.isEmpty()) {
                    continue;
                }
                
                LOGGER.info("Polled {} records", records.count());
                
                for (ConsumerRecord<String, GenericRecord> record : records) {
                    try {
                        processRecord(record);
                        totalProcessed++;
                    } catch (Exception e) {
                        totalErrors++;
                        LOGGER.error("Error processing record at offset {}: {}", 
                            record.offset(), e.getMessage(), e);
                        // 繼續處理下一筆,不中斷整個批次
                    }
                }
                
                // 手動提交 offset
                consumer.commitSync();
                
                if (totalProcessed % 100 == 0) {
                    LOGGER.info("Progress: {} records processed, {} errors", 
                        totalProcessed, totalErrors);
                }
                
            } catch (Exception e) {
                LOGGER.error("Error in consume loop: {}", e.getMessage(), e);
                try {
                    Thread.sleep(5000); // 發生錯誤時等待 5 秒再重試
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        LOGGER.info("Consume loop ended. Total processed: {}, Total errors: {}", 
            totalProcessed, totalErrors);
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "process-kafka-record")
    private void processRecord(ConsumerRecord<String, GenericRecord> record) {
        // 從 Kafka headers 中恢復 Instana trace context
        continueTraceFromHeaders(record.headers());
        
        // 記錄基本資訊
        SpanSupport.annotate("kafka.topic", record.topic());
        SpanSupport.annotate("kafka.partition", String.valueOf(record.partition()));
        SpanSupport.annotate("kafka.offset", String.valueOf(record.offset()));
        SpanSupport.annotate("kafka.key", record.key());
        
        // 反序列化 Avro record
        GenericRecord avroRecord = record.value();
        RawEvent event = deserializeEvent(avroRecord);
        
        // 只處理 checkout 事件
        if ("checkout".equals(event.getEventType())) {
            LOGGER.info("Processing checkout event: order_id={}, user={}, amount={}", 
                event.getOrderId(), event.getUserEmail(), event.getAmount());
            
            processor.process(event);
        } else {
            LOGGER.debug("Skipping non-checkout event: {}", event.getEventType());
        }
    }

    private void continueTraceFromHeaders(Headers headers) {
        Map<String, String> traceHeaders = new HashMap<>();
        
        for (Header header : headers) {
            String key = header.key();
            if (key.startsWith("X-Instana-")) {
                String value = new String(header.value());
                traceHeaders.put(key, value);
            }
        }
        
        if (!traceHeaders.isEmpty()) {
            LOGGER.debug("Continuing trace from headers: {}", traceHeaders);
            // 使用 Instana SDK 繼續 trace
            try {
                String traceId = traceHeaders.get("X-Instana-T");
                String spanId = traceHeaders.get("X-Instana-S");
                if (traceId != null && spanId != null) {
                    SpanSupport.annotate("trace.continued", "true");
                    SpanSupport.annotate("trace.parent_id", traceId);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to continue trace: {}", e.getMessage());
            }
        }
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "deserialize-event")
    private RawEvent deserializeEvent(GenericRecord record) {
        RawEvent event = new RawEvent();
        
        event.setEventType(getStringValue(record, "event_type"));
        event.setEventId(getStringValue(record, "event_id"));
        event.setSessionId(getStringValue(record, "session_id"));
        event.setUserEmail(getStringValue(record, "user_email"));
        event.setUserName(getStringValue(record, "user_name"));
        event.setOrderId(getStringValue(record, "order_id"));
        event.setProductId(getStringValue(record, "product_id"));
        event.setProductName(getStringValue(record, "product_name"));
        event.setProductUrl(getStringValue(record, "product_url"));
        event.setAddress(getStringValue(record, "address"));
        event.setAmount(getIntValue(record, "amount"));
        event.setOrderStatus(getStringValue(record, "order_status"));
        event.setFunnelStep(getStringValue(record, "funnel_step"));
        event.setTs(getLongValue(record, "ts"));
        event.setIsReal(getBooleanValue(record, "is_real"));
        
        return event;
    }

    private String getStringValue(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? value.toString() : null;
    }

    private Integer getIntValue(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? (Integer) value : null;
    }

    private Long getLongValue(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? (Long) value : null;
    }

    private Boolean getBooleanValue(GenericRecord record, String fieldName) {
        Object value = record.get(fieldName);
        return value != null ? (Boolean) value : null;
    }

    public void stop() {
        LOGGER.info("Stopping Order Consumer...");
        running = false;
        
        if (consumer != null) {
            try {
                consumer.close(Duration.ofSeconds(10));
                LOGGER.info("Consumer closed successfully");
            } catch (Exception e) {
                LOGGER.error("Error closing consumer: {}", e.getMessage(), e);
            }
        }
    }
}

// Made with Bob
