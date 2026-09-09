package com.hotelos.web.dto;

import com.hotelos.domain.User;
import com.hotelos.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 32) String phone
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record UserResponse(
            String id,
            String fullName,
            String email,
            String phone,
            UserRole role
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getPublicId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole()
            );
        }
    }

    public record LoginResponse(
            String token,
            String tokenType,
            long expiresInMs,
            UserResponse user
    ) {
        public LoginResponse(String token, long expiresInMs, UserResponse user) {
            this(token, "Bearer", expiresInMs, user);
        }
    }

    public record OAuth2ProviderResponse(
            String id,
            String name,
            String authorizationUrl
    ) {
    }

    public record OAuth2ProvidersResponse(List<OAuth2ProviderResponse> providers) {
    }
}
