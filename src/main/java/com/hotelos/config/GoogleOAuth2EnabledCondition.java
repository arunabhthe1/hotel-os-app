package com.hotelos.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class GoogleOAuth2EnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String clientId = context.getEnvironment().getProperty("hotel.oauth2.google.client-id", "");
        String clientSecret = context.getEnvironment().getProperty("hotel.oauth2.google.client-secret", "");
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
