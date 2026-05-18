package com.example.camping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderPayload {
    @NotBlank
    @JsonProperty("event_type")
    @JsonbProperty("event_type")
    private String eventType;

    @NotBlank
    @JsonProperty("event_id")
    @JsonbProperty("event_id")
    private String eventId;

    @NotBlank
    @JsonProperty("session_id")
    @JsonbProperty("session_id")
    private String sessionId;

    @JsonProperty("user_email")
    @JsonbProperty("user_email")
    private String userEmail;

    @JsonProperty("user_name")
    @JsonbProperty("user_name")
    private String userName;

    @JsonProperty("order_id")
    @JsonbProperty("order_id")
    private String orderId;

    @JsonProperty("product_id")
    @JsonbProperty("product_id")
    private String productId;

    @JsonProperty("product_name")
    @JsonbProperty("product_name")
    private String productName;

    @JsonProperty("product_url")
    @JsonbProperty("product_url")
    private String productUrl;

    private String address;
    private Integer amount;

    @JsonProperty("order_status")
    @JsonbProperty("order_status")
    private String orderStatus;

    @JsonProperty("funnel_step")
    @JsonbProperty("funnel_step")
    private String funnelStep;

    @NotNull
    private Long ts;

    @JsonProperty("is_real")
    @JsonbProperty("is_real")
    private Boolean isReal;

    @JsonProperty("coupon_code")
    @JsonbProperty("coupon_code")
    private String couponCode;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getFunnelStep() {
        return funnelStep;
    }

    public void setFunnelStep(String funnelStep) {
        this.funnelStep = funnelStep;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public Boolean getIsReal() {
        return isReal;
    }

    public void setIsReal(Boolean real) {
        isReal = real;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
