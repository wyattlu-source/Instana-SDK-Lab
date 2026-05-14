package com.example.camping.service;

import com.example.camping.config.AppConfig;
import com.example.camping.dto.OrderPayload;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

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

    @Span(type = Span.Type.EXIT, value = InstanaTracing.KAFKA_SEND_SPAN, captureArguments = true, capturedStackFrames = 5)
    public synchronized void send(@TagParam("order") OrderPayload order) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.KAFKA_SEND_SPAN, KafkaCheckoutService.class.getName(), "send");
        KafkaProducer<String, GenericRecord> currentProducer = getProducer();
        InstanaTracing.kafkaExit(config.kafkaTopicName(), order.getEventId(), order.getEventType());
        ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(
                config.kafkaTopicName(),
                order.getEventId(),
                toGenericRecord(order)
        );
        addInstanaTraceHeaders(record);

        try {
            currentProducer.send(record).get();
            LOGGER.info(() -> "Checkout event sent to Kafka topic " + config.kafkaTopicName() + ": " + order.getEventId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.KAFKA_SEND_SPAN, e);
            throw new IllegalStateException("Interrupted while sending checkout event to Kafka", e);
        } catch (ExecutionException e) {
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.KAFKA_SEND_SPAN, e);
            throw new IllegalStateException("Failed to send checkout event to Kafka", e);
        }
    }

    @Span(type = Span.Type.INTERMEDIATE, value = "camping-kafka-trace-headers", capturedStackFrames = 5)
    private void addInstanaTraceHeaders(ProducerRecord<String, GenericRecord> record) {
        Map<String, String> traceHeaders = new HashMap<>();
        SpanSupport.addTraceHeadersIfTracing(Span.Type.EXIT, traceHeaders);
        InstanaTracing.intermediate("camping-kafka-trace-headers", "tags.trace_header.count", Integer.toString(traceHeaders.size()));
        traceHeaders.forEach((key, value) -> record.headers().add(key, value.getBytes(StandardCharsets.UTF_8)));
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.KAFKA_PRODUCER_INIT_SPAN, capturedStackFrames = 5)
    private KafkaProducer<String, GenericRecord> getProducer() {
        InstanaTracing.method(InstanaTracing.KAFKA_PRODUCER_INIT_SPAN, KafkaCheckoutService.class.getName(), "getProducer");
        InstanaTracing.intermediate(InstanaTracing.KAFKA_PRODUCER_INIT_SPAN, "tags.kafka.bootstrap", config.kafkaBootstrapServer());
        InstanaTracing.intermediate(InstanaTracing.KAFKA_PRODUCER_INIT_SPAN, "tags.kafka.topic", config.kafkaTopicName());
        if (producer == null) {
            Properties properties = new Properties();
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapServer());
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
            properties.put("schema.registry.url", config.schemaRegistryEndpoint());
            properties.put("auto.register.schemas", "false");
            properties.put("use.latest.version", "true");
            producer = new KafkaProducer<>(properties);
        }

        return producer;
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.KAFKA_RECORD_SPAN, captureArguments = true, captureReturn = true, capturedStackFrames = 5)
    private GenericRecord toGenericRecord(@TagParam("order") OrderPayload order) {
        InstanaTracing.method(InstanaTracing.KAFKA_RECORD_SPAN, KafkaCheckoutService.class.getName(), "toGenericRecord");
        InstanaTracing.intermediate(InstanaTracing.KAFKA_RECORD_SPAN, "tags.event.id", order.getEventId());
        InstanaTracing.intermediate(InstanaTracing.KAFKA_RECORD_SPAN, "tags.event.type", order.getEventType());
        InstanaTracing.intermediate(InstanaTracing.KAFKA_RECORD_SPAN, "tags.kafka.topic", config.kafkaTopicName());
        GenericRecord record = new GenericData.Record(RAW_EVENT_SCHEMA);
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
        return record;
    }

    @PreDestroy
    @Span(type = Span.Type.INTERMEDIATE, value = "camping-kafka-producer-close")
    public void close() {
        InstanaTracing.method("camping-kafka-producer-close", KafkaCheckoutService.class.getName(), "close");
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
