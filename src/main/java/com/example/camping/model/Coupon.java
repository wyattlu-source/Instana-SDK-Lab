package com.example.camping.model;

public class Coupon {
    private String couponId;
    private String couponCode;
    private String userId;
    private String spotId;
    private String spotName;
    private int discountAmount;
    private CouponStatus status;
    private long createdAt;
    private long expiresAt;
    private Long usedAt;
    private String orderId;

    public Coupon() {}

    public Coupon(String couponId, String couponCode, String userId, String spotId, 
                  String spotName, int discountAmount, CouponStatus status, 
                  long createdAt, long expiresAt, Long usedAt, String orderId) {
        this.couponId = couponId;
        this.couponCode = couponCode;
        this.userId = userId;
        this.spotId = spotId;
        this.spotName = spotName;
        this.discountAmount = discountAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.orderId = orderId;
    }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }

    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }

    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = discountAmount; }

    public CouponStatus getStatus() { return status; }
    public void setStatus(CouponStatus status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public Long getUsedAt() { return usedAt; }
    public void setUsedAt(Long usedAt) { this.usedAt = usedAt; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

// Made with Bob
