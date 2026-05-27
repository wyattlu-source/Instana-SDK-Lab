package com.example.consumer.notification.config;

public class AppConfig {

    public static String getKafkaBootstrapServer() { return env("KAFKA_BOOTSTRAP_SERVER", "10.107.85.239:9092"); }
    public static String getSchemaRegistryUrl()    { return env("SCHEMA_REGISTRY_URL", "http://10.107.85.239:8081"); }
    public static String getConsumerGroupId()      { return env("CONSUMER_GROUP_ID", "notification-service-group"); }
    public static String getSourceTopic()          { return env("SOURCE_TOPIC", "raw_events"); }
    public static int    getPollTimeoutMs()        { return Integer.parseInt(env("POLL_TIMEOUT_MS", "1000")); }
    public static int    getMaxPollRecords()       { return Integer.parseInt(env("MAX_POLL_RECORDS", "50")); }

    // SMTP 設定
    public static String getSmtpHost()   { return env("SMTP_HOST", "smtp.gmail.com"); }
    public static int    getSmtpPort()   { return Integer.parseInt(env("SMTP_PORT", "587")); }
    public static String getSmtpUser()   { return env("SMTP_USER", ""); }   // Gmail 帳號
    public static String getSmtpPass()   { return env("SMTP_PASS", ""); }   // Gmail App Password
    public static String getEmailFrom()  { return env("EMAIL_FROM", getSmtpUser()); }

    public static void printConfig() {
        System.out.println("=== Notification Service Configuration ===");
        System.out.println("Kafka: " + getKafkaBootstrapServer());
        System.out.println("Schema Registry: " + getSchemaRegistryUrl());
        System.out.println("Consumer Group: " + getConsumerGroupId());
        System.out.println("Topic: " + getSourceTopic());
        System.out.println("SMTP: " + getSmtpHost() + ":" + getSmtpPort());
        System.out.println("SMTP User: " + (getSmtpUser().isEmpty() ? "⚠ 未設定 SMTP_USER" : getSmtpUser()));
        System.out.println("SMTP Pass: " + (getSmtpPass().isEmpty() ? "⚠ 未設定 SMTP_PASS" : "***"));
        System.out.println("==========================================");
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }
}
