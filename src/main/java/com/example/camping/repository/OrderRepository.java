package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.example.camping.model.Order;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OrderRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRepository.class);
    private static final String COLLECTION = "orders";

    @Inject
    MongoConfig mongoConfig;

    @PostConstruct
    public void init() {
        if (!mongoConfig.isAvailable()) return;
        try {
            mongoConfig.getDatabase().getCollection(COLLECTION)
                    .createIndex(new Document("orderId", 1), new IndexOptions().unique(true));
            mongoConfig.getDatabase().getCollection(COLLECTION)
                    .createIndex(new Document("userId", 1).append("createdAt", -1));
        } catch (Exception e) {
            LOGGER.warn("Index creation failed: " + e.getMessage());
        }
    }

    public void save(Order order) {
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[ORDER-DB] save failed - database unavailable, orderId: " + order.getOrderId());
            throw new jakarta.ws.rs.ServiceUnavailableException("資料庫目前無法使用");
        }
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        mongoConfig.getDatabase().getCollection(COLLECTION).insertOne(toDocument(order));
        LOGGER.warn("[ORDER-DB] saved - orderId: " + order.getOrderId() + " userId: " + order.getUserId());
    }

    public List<Order> findByUserId(String userId) {
        if (!mongoConfig.isAvailable()) {
            LOGGER.error("[ORDER-DB] findByUserId failed - database unavailable, userId: " + userId);
            return new ArrayList<>();
        }
        List<Order> orders = new ArrayList<>();
        mongoConfig.getDatabase().getCollection(COLLECTION)
                .find(Filters.eq("userId", userId))
                .sort(new Document("createdAt", -1))
                .forEach(doc -> orders.add(toOrder(doc)));
        LOGGER.warn("[ORDER-DB] found " + orders.size() + " orders for userId: " + userId);
        return orders;
    }

    private Document toDocument(Order o) {
        Document doc = new Document()
                .append("orderId", o.getOrderId())
                .append("userId", o.getUserId())
                .append("userEmail", o.getUserEmail())
                .append("spotId", o.getSpotId())
                .append("spotName", o.getSpotName())
                .append("checkInDate", o.getCheckInDate())
                .append("checkOutDate", o.getCheckOutDate())
                .append("nights", o.getNights())
                .append("unitPrice", o.getUnitPrice())
                .append("total", o.getTotal())
                .append("discountAmount", o.getDiscountAmount())
                .append("finalTotal", o.getFinalTotal())
                .append("status", o.getStatus())
                .append("createdAt", o.getCreatedAt());
        if (o.getCouponCode() != null) doc.append("couponCode", o.getCouponCode());
        return doc;
    }

    private Order toOrder(Document doc) {
        return new Order(
                doc.getString("orderId"),
                doc.getString("userId"),
                doc.getString("userEmail"),
                doc.getString("spotId"),
                doc.getString("spotName"),
                doc.getString("checkInDate"),
                doc.getString("checkOutDate"),
                doc.getInteger("nights", 1),
                doc.getInteger("unitPrice", 0),
                doc.getInteger("total", 0),
                doc.getInteger("discountAmount", 0),
                doc.getInteger("finalTotal", 0),
                doc.getString("couponCode"),
                doc.getString("status"),
                doc.getLong("createdAt")
        );
    }
}
