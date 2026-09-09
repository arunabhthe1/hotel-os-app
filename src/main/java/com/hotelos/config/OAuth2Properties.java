package com.hotelos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hotel.oauth2")
public record OAuth2Properties(Google google, String frontendRedirectUrl) {

    public OAuth2Properties {
        google = google == null ? new Google("", "") : google;
        frontendRedirectUrl = (frontendRedirectUrl == null || frontendRedirectUrl.isBlank())
                ? "http://localhost:5173/login"
                : frontendRedirectUrl;
    }

    public record Google(String clientId, String clientSecret) {
    }

    public boolean googleEnabled() {
        return hasText(google.clientId()) && hasText(google.clientSecret());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
