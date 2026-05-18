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

    public String spotServiceUrl() {
        return env("SPOT_SERVICE_URL", "http://10.107.85.67:8080/spot-service/api");
    }

    public String mongoUri() {
        return env("MONGODB_URI",
                "mongodb://wyattlu25_db_user:%21QAZ2wsx@" +
                "ac-llvix9x-shard-00-00.v90cyby.mongodb.net:27017," +
                "ac-llvix9x-shard-00-01.v90cyby.mongodb.net:27017," +
                "ac-llvix9x-shard-00-02.v90cyby.mongodb.net:27017" +
                "/?authSource=admin&replicaSet=atlas-lgdvwi-shard-0&tls=true&appName=Cluster0");
    }

    public String mongoDatabase() {
        return env("MONGODB_DATABASE", "camping");
    }

    public String jwtSecret() {
        return env("JWT_SECRET", "camping-api-jwt-secret-key-256bit-minimum");
    }

    private String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
