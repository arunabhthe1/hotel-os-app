package com.hotelos.security;

import com.hotelos.config.GoogleOAuth2EnabledCondition;
import com.hotelos.config.OAuth2Properties;
import com.hotelos.service.AuthService;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.AuthDtos.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Conditional(GoogleOAuth2EnabledCondition.class)
@RequiredArgsConstructor
public class CustomerOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            Boolean emailVerified = oauth2User.getAttribute("email_verified");
            if (Boolean.FALSE.equals(emailVerified)) {
                throw ApiException.unauthorized("OAuth2 email is not verified");
            }
            LoginResponse login = authService.completeCustomerOAuth2(
                    oauth2User.getAttribute("email"),
                    oauth2User.getAttribute("name")
            );
            clearSession(request);
            response.sendRedirect(successRedirect(login.token()));
        } catch (ApiException ex) {
            clearSession(request);
            response.sendRedirect(errorRedirect(ex.getMessage()));
        }
    }

    private String successRedirect(String token) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUrl())
                .queryParam("token", token)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    private String errorRedirect(String message) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUrl())
                .queryParam("error", message)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    private String frontendRedirectUrl() {
        return oauth2Properties.frontendRedirectUrl();
    }

    private static void clearSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
