package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.Favorite;
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

        public void save(Favorite favorite) {
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
            throw ex;
        }

        // 生成 favoriteId 如果不存在
        if (favorite.getFavoriteId() == null || favorite.getFavoriteId().isEmpty()) {
            favorite.setFavoriteId(UUID.randomUUID().toString());
        }

        mongoConfig.getDatabase()
                .getCollection(COLLECTION)
                .insertOne(toDocument(favorite));
        
    }

        public List<Favorite> findByUserId(String userId) {
        
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

        public List<Favorite> findActiveByUserId(String userId) {
        
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

        public boolean existsByUserAndSpot(String userId, String spotId) {
        
        if (!mongoConfig.isAvailable()) {
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

        public void cancelFavorite(String userId, String spotId) {
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
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

        public void reactivateFavorite(String userId, String spotId) {
        
        if (!mongoConfig.isAvailable()) {
            jakarta.ws.rs.ServiceUnavailableException ex =
                new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用,請稍後再試");
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

    private void annotateMongoExit(String spanName, String operation) {}

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
