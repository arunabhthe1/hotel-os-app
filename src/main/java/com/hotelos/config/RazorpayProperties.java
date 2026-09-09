package com.hotelos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotel.razorpay")
public record RazorpayProperties(String keyId, String keySecret) {

    public boolean enabled() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }
}
