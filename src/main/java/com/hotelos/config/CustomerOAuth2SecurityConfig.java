package com.hotelos.config;

import com.hotelos.security.CustomerOAuth2FailureHandler;
import com.hotelos.security.CustomerOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Conditional(GoogleOAuth2EnabledCondition.class)
@RequiredArgsConstructor
public class CustomerOAuth2SecurityConfig {

    private final CustomerOAuth2SuccessHandler successHandler;
    private final CustomerOAuth2FailureHandler failureHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain customerOAuth2FilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/api/auth/custlogin/oauth2/authorize/**",
                        "/api/auth/custlogin/oauth2/code/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint ->
                                endpoint.baseUri("/api/auth/custlogin/oauth2/authorize"))
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/api/auth/custlogin/oauth2/code/*"))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                );
        return http.build();
    }
}
