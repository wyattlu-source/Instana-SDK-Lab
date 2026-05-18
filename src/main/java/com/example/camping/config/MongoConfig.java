package com.example.camping.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MongoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConfig.class);

    @Inject
    AppConfig appConfig;

    private MongoClient client;
    private MongoDatabase database;
    private String lastError;

    @PostConstruct
    public void init() {
        try {
            client = MongoClients.create(appConfig.mongoUri());
            database = client.getDatabase(appConfig.mongoDatabase());
            // ping to verify actual connectivity
            database.runCommand(new org.bson.Document("ping", 1));
            LOGGER.info("MongoDB connected: " + appConfig.mongoDatabase());
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOGGER.error("[MONGODB] connection failed: " + lastError, e);
            database = null;
        }
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public boolean isAvailable() {
        return database != null;
    }

    public String getLastError() {
        return lastError;
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
