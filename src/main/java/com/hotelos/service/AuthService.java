package com.hotelos.service;

import com.hotelos.domain.User;
import com.hotelos.domain.enums.UserRole;
import com.hotelos.repository.UserRepository;
import com.hotelos.security.JwtService;
import com.hotelos.security.UserPrincipal;
import com.hotelos.util.PublicIds;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.AuthDtos.LoginRequest;
import com.hotelos.web.dto.AuthDtos.LoginResponse;
import com.hotelos.web.dto.AuthDtos.RegisterRequest;
import com.hotelos.web.dto.AuthDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse registerCustomer(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("An account already exists for this email");
        }

        User user = new User();
        user.setPublicId(PublicIds.next());
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(blankToNull(request.phone()));
        user.setRole(UserRole.customer);
        user.setActive(true);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse customerLogin(LoginRequest request) {
        LoginResponse response = authenticate(request);
        if (response.user().role() != UserRole.customer) {
            throw ApiException.forbidden("This login is for guests only");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public LoginResponse controlledLogin(LoginRequest request) {
        LoginResponse response = authenticate(request);
        UserRole role = response.user().role();
        if (role != UserRole.manager && role != UserRole.admin) {
            throw ApiException.forbidden("This login is for managers and admins only");
        }
        return response;
    }

    @Transactional
    public LoginResponse completeCustomerOAuth2(String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("OAuth2 account did not provide an email");
        }
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createOAuthCustomer(normalizedEmail, fullName));
        if (user.getRole() != UserRole.customer) {
            throw ApiException.forbidden("Staff accounts cannot sign in with OAuth2");
        }
        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is disabled");
        }
        UserPrincipal principal = UserPrincipal.from(user);
        String jwt = jwtService.issueToken(principal);
        return new LoginResponse(jwt, jwtService.expirationMs(), UserResponse.from(user));
    }

    private LoginResponse authenticate(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(email, request.password());
        var authentication = authenticationManager.authenticate(token);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByPublicId(principal.getPublicId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        String jwt = jwtService.issueToken(principal);
        return new LoginResponse(jwt, jwtService.expirationMs(), UserResponse.from(user));
    }

    private User createOAuthCustomer(String email, String fullName) {
        User user = new User();
        user.setPublicId(PublicIds.next());
        user.setFullName(fullName == null || fullName.isBlank() ? email : fullName.trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(UserRole.customer);
        user.setActive(true);
        return userRepository.save(user);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
