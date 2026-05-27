package com.example.consumer.order.model;

/**
 * RawEvent Model
 * 對應 Kafka raw_events topic 的 Avro Schema
 */
public class RawEvent {
    private String eventType;
    private String eventId;
    private String sessionId;
    private String userEmail;
    private String userName;
    private String orderId;
    private String productId;
    private String productName;
    private String productUrl;
    private String address;
    private Integer amount;
    private String orderStatus;
    private String funnelStep;
    private Long ts;
    private Boolean isReal;

    public RawEvent() {
    }

    // Getters and Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getFunnelStep() { return funnelStep; }
    public void setFunnelStep(String funnelStep) { this.funnelStep = funnelStep; }

    public Long getTs() { return ts; }
    public void setTs(Long ts) { this.ts = ts; }

    public Boolean getIsReal() { return isReal; }
    public void setIsReal(Boolean isReal) { this.isReal = isReal; }

    @Override
    public String toString() {
        return "RawEvent{" +
                "eventType='" + eventType + '\'' +
                ", eventId='" + eventId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", productName='" + productName + '\'' +
                ", amount=" + amount +
                ", orderStatus='" + orderStatus + '\'' +
                '}';
    }
}

// Made with Bob
