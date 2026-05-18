package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.Favorite;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FavoriteRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteRepository.class);
    private static final String COLLECTION = "favorites";

    @Inject
    MongoConfig mongoConfig;

    @PostConstruct
    public void init() {
        if (!mongoConfig.isAvailable()) return;
        try {
            // 複合唯一索引: {userId, spotId}
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("userId", 1).append("spotId", 1),
                            new IndexOptions().unique(true)
                    );

            // 查詢索引: {userId, active, createdAt}
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("userId", 1)
                                    .append("active", 1)
                                    .append("createdAt", -1)
                    );

            // 統計索引: {spotId, active}
            mongoConfig.getDatabase()
                    .getCollection(COLLECTION)
                    .createIndex(
                            new Document("spotId", 1).append("active", 1)
                    );
        } catch (Exception e) {
            LOGGER.warn("Index creation failed: " + e.getMessage());
        }
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_SAVE_SPAN, capturedStackFrames = 5)
    public void save(@TagParam("favorite") Favorite favorite) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_SAVE_SPAN, 
                FavoriteRepository.class.getName(), "save");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_SAVE_SPAN, "insertOne");
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            LOGGER.error("[MONGODB] save failed - database unavailable, favoriteId: " + 
                    favorite.getFavoriteId(), ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_SAVE_SPAN, ex);
            throw ex;
        }

        // 生成 favoriteId 如果不存在
        if (favorite.getFavoriteId() == null || favorite.getFavoriteId().isEmpty()) {
            favorite.setFavoriteId(UUID.randomUUID().toString());
        }

        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .insertOne(toDocument(favorite));
        
        LOGGER.warn("[MONGODB] Favorite saved - favoriteId: " + favorite.getFavoriteId() +
                ", userId: " + favorite.getUserId() + ", spotId: " + favorite.getSpotId());
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_FIND_BY_USER_SPAN, capturedStackFrames = 5)
    public List<Favorite> findByUserId(@TagParam("userId") String userId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_FIND_BY_USER_SPAN,
                FavoriteRepository.class.getName(), "findByUserId");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_FIND_BY_USER_SPAN, "find");
        
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findByUserId failed - database unavailable, userId: " + userId);
            return new ArrayList<>();
        }

        List<Favorite> favorites = new ArrayList<>();
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(Filters.eq("userId", userId))
                .into(new ArrayList<>())
                .forEach(doc -> favorites.add(toFavorite(doc)));
        
        return favorites;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_FIND_ACTIVE_SPAN, capturedStackFrames = 5)
    public List<Favorite> findActiveByUserId(@TagParam("userId") String userId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_FIND_ACTIVE_SPAN,
                FavoriteRepository.class.getName(), "findActiveByUserId");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_FIND_ACTIVE_SPAN, "find");
        
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] findActiveByUserId failed - database unavailable, userId: " + userId);
            return new ArrayList<>();
        }

        List<Favorite> favorites = new ArrayList<>();
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("active", true)
        );
        
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .find(filter)
                .sort(new Document("createdAt", -1))
                .into(new ArrayList<>())
                .forEach(doc -> favorites.add(toFavorite(doc)));
        
        LOGGER.warn("[MONGODB] Found " + favorites.size() + " active favorites for userId: " + userId);
        return favorites;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_EXISTS_SPAN, capturedStackFrames = 5)
    public boolean existsByUserAndSpot(@TagParam("userId") String userId, @TagParam("spotId") String spotId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_EXISTS_SPAN,
                FavoriteRepository.class.getName(), "existsByUserAndSpot");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_EXISTS_SPAN, "count");
        
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[MONGODB] existsByUserAndSpot failed - database unavailable, userId: " + 
                    userId + ", spotId: " + spotId);
            return false;
        }

        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("spotId", spotId)
        );
        
        return mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .countDocuments(filter) > 0;
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_CANCEL_SPAN, capturedStackFrames = 5)
    public void cancelFavorite(@TagParam("userId") String userId, @TagParam("spotId") String spotId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_CANCEL_SPAN,
                FavoriteRepository.class.getName(), "cancelFavorite");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_CANCEL_SPAN, "updateOne");
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            LOGGER.error("[MONGODB] cancelFavorite failed - database unavailable, userId: " + 
                    userId + ", spotId: " + spotId, ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_CANCEL_SPAN, ex);
            throw ex;
        }

        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("spotId", spotId)
        );
        
        Bson update = Updates.combine(
                Updates.set("active", false),
                Updates.set("canceledAt", System.currentTimeMillis())
        );
        
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .updateOne(filter, update);
        
        LOGGER.warn("[MONGODB] Favorite canceled - userId: " + userId + ", spotId: " + spotId);
    }

    @Span(type = Span.Type.EXIT, value = InstanaTracing.FAVORITE_REPO_REACTIVATE_SPAN, capturedStackFrames = 5)
    public void reactivateFavorite(@TagParam("userId") String userId, @TagParam("spotId") String spotId) {
        InstanaTracing.method(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_REACTIVATE_SPAN,
                FavoriteRepository.class.getName(), "reactivateFavorite");
        annotateMongoExit(InstanaTracing.FAVORITE_REPO_REACTIVATE_SPAN, "updateOne");
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            LOGGER.error("[MONGODB] reactivateFavorite failed - database unavailable, userId: " + 
                    userId + ", spotId: " + spotId, ex);
            InstanaTracing.error(Span.Type.EXIT, InstanaTracing.FAVORITE_REPO_REACTIVATE_SPAN, ex);
            throw ex;
        }

        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("spotId", spotId)
        );
        
        Bson update = Updates.combine(
                Updates.set("active", true),
                Updates.set("canceledAt", null)
        );
        
        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .updateOne(filter, update);
        
        LOGGER.warn("[MONGODB] Favorite reactivated - userId: " + userId + ", spotId: " + spotId);
    }

    private void annotateMongoExit(String spanName, String operation) {
        com.instana.sdk.support.SpanSupport.annotate("tags.db.type", "mongodb");
        com.instana.sdk.support.SpanSupport.annotate("tags.db.collection", COLLECTION);
        com.instana.sdk.support.SpanSupport.annotate("tags.db.operation", operation);
        com.instana.sdk.support.SpanSupport.annotate("tags.service", "camping-api");
    }

    private Favorite toFavorite(Document doc) {
        return new Favorite(
                doc.getString("favoriteId"),
                doc.getString("userId"),
                doc.getString("spotId"),
                doc.getString("spotName"),
                doc.getLong("createdAt"),
                doc.getBoolean("active", true),
                doc.getLong("canceledAt")
        );
    }

    private Document toDocument(Favorite favorite) {
        Document doc = new Document()
                .append("favoriteId", favorite.getFavoriteId())
                .append("userId", favorite.getUserId())
                .append("spotId", favorite.getSpotId())
                .append("spotName", favorite.getSpotName())
                .append("createdAt", favorite.getCreatedAt())
                .append("active", favorite.isActive());
        
        if (favorite.getCanceledAt() != null) {
            doc.append("canceledAt", favorite.getCanceledAt());
        }
        
        return doc;
    }
}

// Made with Bob
