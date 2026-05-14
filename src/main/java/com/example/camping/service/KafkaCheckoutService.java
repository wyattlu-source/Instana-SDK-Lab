package com.example.camping.service;

import com.example.camping.config.AppConfig;
import com.example.camping.dto.OrderPayload;
import com.example.camping.util.InstanaTracingUtil;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Kafka Checkout Service
 *
 * 追蹤層級：
 * 1. Instana Agent 自動追蹤：Kafka Producer 調用、網路 I/O、序列化
 * 2. SDK 手動追蹤：訊息內容、處理步驟、業務邏輯
 */
@ApplicationScoped
public class KafkaCheckoutService {
    private static final Logger LOGGER = Logger.getLogger(KafkaCheckoutService.class.getName());
    private static final Schema RAW_EVENT_SCHEMA = new Schema.Parser().parse("""
            {
              "type": "record",
              "name": "RawEvent",
              "namespace": "com.travel.events",
              "fields": [
                {"name": "event_type", "type": "string"},
                {"name": "event_id", "type": "string"},
                {"name": "session_id", "type": "string"},
                {"name": "user_email", "type": ["null", "string"], "default": null},
                {"name": "user_name", "type": ["null", "string"], "default": null},
                {"name": "order_id", "type": ["null", "string"], "default": null},
                {"name": "product_id", "type": ["null", "string"], "default": null},
                {"name": "product_name", "type": ["null", "string"], "default": null},
                {"name": "product_url", "type": ["null", "string"], "default": null},
                {"name": "address", "type": ["null", "string"], "default": null},
                {"name": "amount", "type": ["null", "int"], "default": null},
                {"name": "order_status", "type": ["null", "string"], "default": null},
                {"name": "funnel_step", "type": ["null", "string"], "default": null},
                {"name": "ts", "type": "long", "logicalType": "timestamp-millis"},
                {"name": "is_real", "type": ["null", "boolean"], "default": null}
              ]
            }
            """);

    @Inject
    AppConfig config;

    private KafkaProducer<String, GenericRecord> producer;

    /**
     * Agent 自動追蹤：Kafka Producer.send() 調用、網路延遲、序列化時間
     * SDK 手動追蹤：訊息內容和大小、處理步驟、業務邏輯狀態
     */
    @Span(value = "kafka.send", type = Span.Type.EXIT)
    public synchronized void send(OrderPayload order) {
        InstanaTracingUtil.traceVoid("KafkaCheckoutService.send", () -> {
            
            // === 步驟 1：獲取 Kafka Producer ===
            InstanaTracingUtil.markStep("1.get_producer", "獲取或初始化 Kafka Producer");
            
            KafkaProducer<String, GenericRecord> currentProducer =
                InstanaTracingUtil.trace("KafkaCheckoutService.getProducer", this::getProducer);
            
            InstanaTracingUtil.addBusinessTag("kafka.producer.initialized", currentProducer != null);
            
            // === 步驟 2：轉換為 Avro GenericRecord ===
            InstanaTracingUtil.markStep("2.convert_to_avro", "將訂單轉換為 Avro 格式");
            
            GenericRecord avroRecord = InstanaTracingUtil.trace(
                "KafkaCheckoutService.toGenericRecord",
                () -> toGenericRecord(order)
            );
            
            InstanaTracingUtil.addBusinessTag("avro.schema.name", RAW_EVENT_SCHEMA.getName());
            InstanaTracingUtil.addBusinessTag("avro.schema.namespace", RAW_EVENT_SCHEMA.getNamespace());
            
            // === 步驟 3：創建 ProducerRecord ===
            InstanaTracingUtil.markStep("3.create_record", "創建 Kafka ProducerRecord");
            
            String topic = config.kafkaTopicName();
            String key = order.getEventId();
            
            InstanaTracingUtil.addBusinessTag("kafka.topic", topic);
            InstanaTracingUtil.addBusinessTag("kafka.key", key);
            InstanaTracingUtil.addBusinessTag("kafka.bootstrap_server", config.kafkaBootstrapServer());
            InstanaTracingUtil.addBusinessTag("kafka.schema_registry", config.schemaRegistryEndpoint());
            
            ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(topic, key, avroRecord);
            
            // === 步驟 4：發送到 Kafka ===
            InstanaTracingUtil.markStep("4.send_to_kafka", "發送訊息到 Kafka Broker");
            
            try {
                long sendStartTime = System.currentTimeMillis();
                
                // Agent 會自動追蹤這個 Kafka 調用
                currentProducer.send(record).get();
                
                long sendDuration = System.currentTimeMillis() - sendStartTime;
                
                // 添加業務指標
                InstanaTracingUtil.addBusinessTag("kafka.send.duration_ms", sendDuration);
                InstanaTracingUtil.addBusinessTag("kafka.send.success", true);
                InstanaTracingUtil.addBusinessTag("message.size_bytes", avroRecord.toString().length());
                
                InstanaTracingUtil.logBusinessEvent("KAFKA_MESSAGE_SENT",
                    String.format("Message sent to topic %s with key %s in %dms",
                        topic, key, sendDuration));
                
                LOGGER.info(String.format(
                    "[INSTANA-KAFKA] Message sent successfully - Topic: %s, Key: %s, Duration: %dms",
                    topic, key, sendDuration
                ));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                
                InstanaTracingUtil.addBusinessTag("kafka.send.success", false);
                InstanaTracingUtil.addBusinessTag("error.type", "InterruptedException");
                InstanaTracingUtil.addBusinessTag("error.message", e.getMessage());
                
                InstanaTracingUtil.logBusinessEvent("KAFKA_SEND_ERROR",
                    "Interrupted while sending to Kafka: " + e.getMessage());
                
                throw new IllegalStateException("Interrupted while sending checkout event to Kafka", e);
                
            } catch (ExecutionException e) {
                InstanaTracingUtil.addBusinessTag("kafka.send.success", false);
                InstanaTracingUtil.addBusinessTag("error.type", "ExecutionException");
                InstanaTracingUtil.addBusinessTag("error.message", e.getMessage());
                InstanaTracingUtil.addBusinessTag("error.cause",
                    e.getCause() != null ? e.getCause().getMessage() : "unknown");
                
                InstanaTracingUtil.logBusinessEvent("KAFKA_SEND_ERROR",
                    "Failed to send to Kafka: " + e.getMessage());
                
                throw new IllegalStateException("Failed to send checkout event to Kafka", e);
            }
        });
    }

    private KafkaProducer<String, GenericRecord> getProducer() {
        return InstanaTracingUtil.trace("KafkaCheckoutService.initializeProducer", () -> {
            if (producer == null) {
                InstanaTracingUtil.markStep("initialize_producer", "初始化新的 Kafka Producer");
                
                Properties properties = new Properties();
                properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServer());
                properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
                properties.put("schema.registry.url", config.schemaRegistryEndpoint());
                properties.put("auto.register.schemas", "false");
                properties.put("use.latest.version", "true");
                
                producer = new KafkaProducer<>(properties);
                
                InstanaTracingUtil.addBusinessTag("producer.status", "newly_initialized");
                InstanaTracingUtil.logBusinessEvent("KAFKA_PRODUCER_INITIALIZED",
                    "New Kafka producer created");
            } else {
                InstanaTracingUtil.addBusinessTag("producer.status", "reused");
            }
            return producer;
        });
    }

    private GenericRecord toGenericRecord(OrderPayload order) {
        return InstanaTracingUtil.trace("KafkaCheckoutService.mapOrderToAvro", () -> {
            GenericRecord record = new GenericData.Record(RAW_EVENT_SCHEMA);
            
            InstanaTracingUtil.traceVoid("KafkaCheckoutService.setAvroFields", () -> {
                record.put("event_type", order.getEventType());
                record.put("event_id", order.getEventId());
                record.put("session_id", order.getSessionId());
                record.put("user_email", order.getUserEmail());
                record.put("user_name", order.getUserName());
                record.put("order_id", order.getOrderId());
                record.put("product_id", order.getProductId());
                record.put("product_name", order.getProductName());
                record.put("product_url", order.getProductUrl());
                record.put("address", order.getAddress());
                record.put("amount", order.getAmount());
                record.put("order_status", order.getOrderStatus());
                record.put("funnel_step", order.getFunnelStep());
                record.put("ts", order.getTs());
                record.put("is_real", order.getIsReal());
            });
            
            InstanaTracingUtil.addBusinessTag("avro.fields_count", 15);
            InstanaTracingUtil.logBusinessEvent("AVRO_RECORD_CREATED",
                "OrderPayload converted to Avro GenericRecord");
            
            return record;
        });
    }

    @PreDestroy
    public void close() {
        InstanaTracingUtil.traceVoid("KafkaCheckoutService.close", () -> {
            if (producer != null) {
                InstanaTracingUtil.markStep("close_producer", "關閉 Kafka Producer");
                
                producer.flush();
                InstanaTracingUtil.addBusinessTag("producer.flushed", true);
                
                producer.close();
                InstanaTracingUtil.addBusinessTag("producer.closed", true);
                
                InstanaTracingUtil.logBusinessEvent("KAFKA_PRODUCER_CLOSED",
                    "Kafka producer closed successfully");
            }
        });
    }
}
