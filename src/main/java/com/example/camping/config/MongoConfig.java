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
    private String errorType;

    @PostConstruct
    public void init() {
        try {
            client = MongoClients.create(appConfig.mongoUri());
            database = client.getDatabase(appConfig.mongoDatabase());
            // ping to verify actual connectivity
            database.runCommand(new org.bson.Document("ping", 1));
            LOGGER.info("MongoDB connected: " + appConfig.mongoDatabase());
        } catch (Exception e) {
            errorType = classifyMongoError(e);
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOGGER.error("[MONGODB] connection failed [{}]: {}", errorType, lastError, e);
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

    public String getErrorType() {
        return errorType;
    }

    public String getErrorHint() {
        if (errorType == null) return null;
        switch (errorType) {
            case "auth_failure":       return "MongoDB 帳號密碼錯誤，請確認 MONGODB_URI 中的用戶名稱與密碼";
            case "timeout":            return "MongoDB 連線逾時，服務可能未啟動或主機不可達";
            case "connection_refused": return "MongoDB 服務未啟動（Connection refused），請確認 mongod 狀態";
            default:                   return "MongoDB 連線失敗，請查看伺服器日誌";
        }
    }

    private String classifyMongoError(Exception e) {
        Throwable t = e;
        while (t != null) {
            String cn  = t.getClass().getName().toLowerCase();
            String msg = t.getMessage() != null ? t.getMessage().toLowerCase() : "";
            if (cn.contains("authenticationexception")
                    || msg.contains("authentication failed")
                    || msg.contains("bad auth")
                    || msg.contains("sasl")
                    || msg.contains("credential"))          return "auth_failure";
            if (cn.contains("mongotimeoutexception")
                    || cn.contains("sockettimeoutexception")
                    || msg.contains("timed out")
                    || msg.contains("server selection"))    return "timeout";
            if (cn.contains("connectexception")
                    || msg.contains("connection refused"))  return "connection_refused";
            t = t.getCause();
        }
        return "unknown";
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
