package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.User;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
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

    @Span(type = Span.Type.EXIT, value = InstanaTracing.USER_REPO_FIND_SPAN, capturedStackFrames = 5)
    public Optional<User> findByEmail(@TagParam("email") String email) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.USER_REPO_FIND_SPAN, UserRepository.class.getName(), "findByEmail");
        annotateMongoExit(InstanaTracing.USER_REPO_FIND_SPAN, "find");
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findByEmail failed - database unavailable, email: " + email);
            return Optional.empty();
        }
        Document doc = mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(Filters.eq("email", email))
                .first();
        return Optional.ofNullable(doc).map(this::toUser);
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.USER_REPO_EXISTS_SPAN, capturedStackFrames = 5)
    public boolean existsByEmail(@TagParam("email") String email) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.USER_REPO_EXISTS_SPAN, UserRepository.class.getName(), "existsByEmail");
        annotateMongoExit(InstanaTracing.USER_REPO_EXISTS_SPAN, "count");
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] existsByEmail failed - database unavailable, email: " + email);
            return false;
        }
        return mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .countDocuments(Filters.eq("email", email)) > 0;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.USER_REPO_SAVE_SPAN, capturedStackFrames = 5)
    public void save(@TagParam("user") User user) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.USER_REPO_SAVE_SPAN, UserRepository.class.getName(), "save");
        annotateMongoExit(InstanaTracing.USER_REPO_SAVE_SPAN, "insertOne");
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用，請稍後再試");
            LOGGER.error("[MONGODB] save failed - database unavailable, userId: " + user.getUserId(), ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.USER_REPO_SAVE_SPAN, ex);
            throw ex;
        }
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .insertOne(toDocument(user));
    }

    private void annotateMongoExit(String spanName, String operation) {
        com.instana.sdk.support.SpanSupport.annotate("tags.db.type", "mongodb");
        com.instana.sdk.support.SpanSupport.annotate("tags.db.collection", COLLECTION);
        com.instana.sdk.support.SpanSupport.annotate("tags.db.operation", operation);
        com.instana.sdk.support.SpanSupport.annotate("tags.service", "camping-api");
    }

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
