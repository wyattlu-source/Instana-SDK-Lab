package com.example.consumer.notification.model;

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

    // Getters / Setters
    public String getEventType()   { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public String getEventId()     { return eventId; }
    public void setEventId(String v)   { this.eventId = v; }
    public String getSessionId()   { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getUserEmail()   { return userEmail; }
    public void setUserEmail(String v) { this.userEmail = v; }
    public String getUserName()    { return userName; }
    public void setUserName(String v)  { this.userName = v; }
    public String getOrderId()     { return orderId; }
    public void setOrderId(String v)   { this.orderId = v; }
    public String getProductId()   { return productId; }
    public void setProductId(String v) { this.productId = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v){ this.productName = v; }
    public String getProductUrl()  { return productUrl; }
    public void setProductUrl(String v){ this.productUrl = v; }
    public String getAddress()     { return address; }
    public void setAddress(String v)   { this.address = v; }
    public Integer getAmount()     { return amount; }
    public void setAmount(Integer v)   { this.amount = v; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String v){ this.orderStatus = v; }
    public String getFunnelStep()  { return funnelStep; }
    public void setFunnelStep(String v){ this.funnelStep = v; }
    public Long getTs()            { return ts; }
    public void setTs(Long v)      { this.ts = v; }
    public Boolean getIsReal()     { return isReal; }
    public void setIsReal(Boolean v)   { this.isReal = v; }
}
