package com.example.consumer.order.config;

/**
 * Application Configuration
 * 從環境變數讀取配置
 */
public class AppConfig {
    
    public static String getKafkaBootstrapServer() {
        return getEnv("KAFKA_BOOTSTRAP_SERVER", "10.107.85.239:9092");
    }
    
    public static String getSchemaRegistryUrl() {
        return getEnv("SCHEMA_REGISTRY_URL", "http://10.107.85.239:8081");
    }
    
    public static String getConsumerGroupId() {
        return getEnv("CONSUMER_GROUP_ID", "order-processor-group");
    }
    
    public static String getSourceTopic() {
        return getEnv("SOURCE_TOPIC", "raw_events");
    }
    
    public static String getTargetTopic() {
        return getEnv("TARGET_TOPIC", "orders_processed");
    }
    
    public static int getPollTimeoutMs() {
        return Integer.parseInt(getEnv("POLL_TIMEOUT_MS", "1000"));
    }
    
    public static int getMaxPollRecords() {
        return Integer.parseInt(getEnv("MAX_POLL_RECORDS", "100"));
    }
    
    public static String getInstanaAgentHost() {
        return getEnv("INSTANA_AGENT_HOST", "localhost");
    }
    
    public static int getInstanaAgentPort() {
        return Integer.parseInt(getEnv("INSTANA_AGENT_PORT", "42699"));
    }
    
    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
    
    public static void printConfig() {
        System.out.println("=== Configuration ===");
        System.out.println("Kafka Bootstrap Server: " + getKafkaBootstrapServer());
        System.out.println("Schema Registry URL: " + getSchemaRegistryUrl());
        System.out.println("Consumer Group ID: " + getConsumerGroupId());
        System.out.println("Source Topic: " + getSourceTopic());
        System.out.println("Target Topic: " + getTargetTopic());
        System.out.println("Poll Timeout (ms): " + getPollTimeoutMs());
        System.out.println("Max Poll Records: " + getMaxPollRecords());
        System.out.println("Instana Agent: " + getInstanaAgentHost() + ":" + getInstanaAgentPort());
        System.out.println("====================");
    }
}

// Made with Bob
