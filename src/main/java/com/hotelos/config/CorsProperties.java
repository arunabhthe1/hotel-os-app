package com.hotelos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "hotel.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
