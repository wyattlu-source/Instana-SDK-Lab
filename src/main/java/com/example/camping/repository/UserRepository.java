package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.User;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class UserRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);
    private static final String COLLECTION = "users";

    @Inject
    MongoConfig mongoConfig;

    @PostConstruct
    public void init() {
        if (!mongoConfig.isAvailable()) return;
        try {
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(new Document("email", 1), new IndexOptions().unique(true));
        } catch (Exception e) {
            LOGGER.warn("Index creation failed: " + e.getMessage());
        }
    }

    public Optional<User> findByEmail(String email) {
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findByEmail failed [{}], email: {}", mongoConfig.getErrorType(), email);
            annotateMongoUnavailable();
            return Optional.empty();
        }
        Document doc = mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(Filters.eq("email", email))
                .first();
        return Optional.ofNullable(doc).map(this::toUser);
    }

    public boolean existsByEmail(String email) {
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] existsByEmail failed [{}], email: {}", mongoConfig.getErrorType(), email);
            annotateMongoUnavailable();
            return false;
        }
        return mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .countDocuments(Filters.eq("email", email)) > 0;
    }

    public void save(User user) {
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用，請稍後再試");
            LOGGER.error("[MONGODB] save failed [{}], userId: {}", mongoConfig.getErrorType(), user.getUserId(), ex);
            annotateMongoUnavailable();
            throw ex;
        }
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .insertOne(toDocument(user));
    }

    private void annotateMongoExit(String spanName, String operation) {}

    private void annotateMongoUnavailable() {}

    private User toUser(Document doc) {
        return new User(
                doc.getString("userId"),
                doc.getString("name"),
                doc.getString("email"),
                doc.getString("passwordHash"),
                doc.getLong("createdAt")
        );
    }

    private Document toDocument(User user) {
        return new Document()
                .append("userId", user.getUserId())
                .append("name", user.getName())
                .append("email", user.getEmail())
                .append("passwordHash", user.getPasswordHash())
                .append("createdAt", user.getCreatedAt());
    }
}
