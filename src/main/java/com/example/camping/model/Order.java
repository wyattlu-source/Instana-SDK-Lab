package com.example.camping.model;

public class Order {
    private String orderId;
    private String userId;
    private String userEmail;
    private String spotId;
    private String spotName;
    private String checkInDate;
    private String checkOutDate;
    private int nights;
    private int unitPrice;
    private int total;
    private int discountAmount;
    private int finalTotal;
    private String couponCode;
    private String status;
    private long createdAt;

    public Order() {}

    public Order(String orderId, String userId, String userEmail,
                 String spotId, String spotName,
                 String checkInDate, String checkOutDate, int nights,
                 int unitPrice, int total, int discountAmount, int finalTotal,
                 String couponCode, String status, long createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.spotId = spotId;
        this.spotName = spotName;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.unitPrice = unitPrice;
        this.total = total;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.couponCode = couponCode;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getSpotId() { return spotId; }
    public void setSpotId(String spotId) { this.spotId = spotId; }
    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }
    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }
    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = discountAmount; }
    public int getFinalTotal() { return finalTotal; }
    public void setFinalTotal(int finalTotal) { this.finalTotal = finalTotal; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
