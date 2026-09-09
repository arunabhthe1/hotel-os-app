package com.hotelos.web;

import com.hotelos.security.CurrentUserService;
import com.hotelos.service.AuthService;
import com.hotelos.web.dto.AuthDtos.LoginRequest;
import com.hotelos.web.dto.AuthDtos.LoginResponse;
import com.hotelos.web.dto.AuthDtos.OAuth2ProviderResponse;
import com.hotelos.web.dto.AuthDtos.OAuth2ProvidersResponse;
import com.hotelos.web.dto.AuthDtos.RegisterRequest;
import com.hotelos.web.dto.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CUSTOMER_OAUTH2_AUTHORIZE_BASE = "/api/auth/custlogin/oauth2/authorize";

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/custlogin")
    public LoginResponse customerLogin(@Valid @RequestBody LoginRequest request) {
        return authService.customerLogin(request);
    }

    @GetMapping("/custlogin/oauth2")
    public OAuth2ProvidersResponse customerOAuth2Providers() {
        ClientRegistrationRepository repository = clientRegistrations.getIfAvailable();
        if (!(repository instanceof Iterable<?> registrations)) {
            return new OAuth2ProvidersResponse(List.of());
        }

        List<OAuth2ProviderResponse> providers = new ArrayList<>();
        for (Object item : registrations) {
            if (item instanceof ClientRegistration registration) {
                providers.add(new OAuth2ProviderResponse(
                        registration.getRegistrationId(),
                        registration.getClientName(),
                        CUSTOMER_OAUTH2_AUTHORIZE_BASE + "/" + registration.getRegistrationId()
                ));
            }
        }
        return new OAuth2ProvidersResponse(List.copyOf(providers));
    }

    @PostMapping("/contolledlogin")
    public LoginResponse controlledLogin(@Valid @RequestBody LoginRequest request) {
        return authService.controlledLogin(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUserService.entity());
    }
}
