package com.example.camping.config;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppConfig {
    public String kafkaBootstrapServer() {
        return env("KAFKA_BOOTSTRAP_SERVER", "10.107.85.239:9092");
    }

    public String schemaRegistryEndpoint() {
        return env("SCHEMA_REGISTRY_ENDPOINT", "http://10.107.85.239:8081");
    }

    public String ksqlDbEndpoint() {
        return env("KSQLDB_ENDPOINT", "http://10.107.85.239:8088");
    }

    public String kafkaTopicName() {
        return env("KAFKA_TOPIC_NAME", "raw_events");
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
