package com.example.camping.model;

public enum CouponStatus {
    UNUSED("UNUSED"),
    USED("USED"),
    EXPIRED("EXPIRED");

    private final String value;

    CouponStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CouponStatus fromValue(String value) {
        for (CouponStatus status : CouponStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown coupon status: " + value);
    }
}

// Made with Bob
