package com.example.camping.repository;

import com.example.camping.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProcessingStatusRepository {

    @Inject
    MongoConfig mongoConfig;

    public List<Map<String, Object>> findAll() {
        if (!mongoConfig.isAvailable()) return new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();
        mongoConfig.getDatabase().getCollection("processing_logs")
                .find()
                .sort(new Document("processed_at", -1))
                .forEach(doc -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("order_id",     doc.getString("order_id"));
                    result.put("event_type",   doc.getString("event_type"));
                    result.put("user_email",   doc.getString("user_email"));
                    result.put("amount",       doc.getInteger("amount", 0));
                    result.put("processed_at", doc.getLong("processed_at"));
                    result.put("notification", toMap(doc.get("notification", Document.class)));
                    result.put("inventory",    toMap(doc.get("inventory",    Document.class)));
                    result.put("billing",      toMap(doc.get("billing",      Document.class)));
                    result.put("analytics",    toMap(doc.get("analytics",    Document.class)));
                    results.add(result);
                });
        return results;
    }

    public Map<String, Object> findByOrderId(String orderId) {
        if (!mongoConfig.isAvailable()) return null;

        MongoCollection<Document> col = mongoConfig.getDatabase().getCollection("processing_logs");
        Document doc = col.find(new Document("_id", orderId)).first();
        if (doc == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("order_id",     doc.getString("order_id"));
        result.put("event_type",   doc.getString("event_type"));
        result.put("user_email",   doc.getString("user_email"));
        result.put("amount",       doc.getInteger("amount", 0));
        result.put("processed_at", doc.getLong("processed_at"));
        result.put("notification", toMap(doc.get("notification", Document.class)));
        result.put("inventory",    toMap(doc.get("inventory",    Document.class)));
        result.put("billing",      toMap(doc.get("billing",      Document.class)));
        result.put("analytics",    toMap(doc.get("analytics",    Document.class)));
        return result;
    }

    private Map<String, Object> toMap(Document d) {
        if (d == null) return Map.of("status", "pending");
        Map<String, Object> m = new HashMap<>(d);
        m.remove("_id");
        return m;
    }
}
