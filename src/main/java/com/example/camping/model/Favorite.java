package com.example.camping.model;

public class Favorite {
    private String favoriteId;
    private String userId;
    private String spotId;
    private String spotName;
    private long createdAt;
    private boolean active;
    private Long canceledAt;

    public Favorite() {}

    public Favorite(String favoriteId, String userId, String spotId, String spotName, 
                    long createdAt, boolean active, Long canceledAt) {
        this.favoriteId = favoriteId;
        this.userId = userId;
        this.spotId = spotId;
        this.spotName = spotName;
        this.createdAt = createdAt;
        this.active = active;
        this.canceledAt = canceledAt;
    }

    public String getFavoriteId() { return favoriteId; }
    public void setFavoriteId(String favoriteId) { this.favoriteId = favoriteId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public Long getCanceledAt() { return canceledAt; }
    public void setCanceledAt(Long canceledAt) { this.canceledAt = canceledAt; }
}

// Made with Bob
