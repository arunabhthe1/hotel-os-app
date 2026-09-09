package com.hotelos.security;

import com.hotelos.config.GoogleOAuth2EnabledCondition;
import com.hotelos.config.OAuth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Conditional(GoogleOAuth2EnabledCondition.class)
@RequiredArgsConstructor
public class CustomerOAuth2FailureHandler implements AuthenticationFailureHandler {

    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(UriComponentsBuilder.fromUriString(oauth2Properties.frontendRedirectUrl())
                .queryParam("error", "oauth2_login_failed")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString());
    }
}
