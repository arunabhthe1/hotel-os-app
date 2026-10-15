package com.hotelos.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@ConfigurationProperties(prefix = "hotel.oauth2")
public record OAuth2Properties(Google google, String frontendRedirectUrl, boolean useRequestOrigin) {

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

    /**
     * On Cloud Run the UI is the same origin as the API, so redirect using the public request URL.
     * Locally the Vite app is on :5173 and the API on :8080.
     */
    public String resolveFrontendRedirectUrl(HttpServletRequest request) {
        if (useRequestOrigin) {
            return ServletUriComponentsBuilder.fromRequest(request)
                    .replacePath("/login")
                    .replaceQuery(null)
                    .fragment(null)
                    .build()
                    .toUriString();
        }
        return frontendRedirectUrl;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
