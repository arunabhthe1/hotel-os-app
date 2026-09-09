package com.hotelos.security;

import com.hotelos.domain.User;
import com.hotelos.repository.UserRepository;
import com.hotelos.web.advice.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw ApiException.unauthorized("Authentication required");
        }
        return principal;
    }

    public User entity() {
        return userRepository.findById(principal().getId())
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
    }
}
