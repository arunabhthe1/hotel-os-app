package com.hotelos.security;

import com.hotelos.config.JwtProperties;
import com.hotelos.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void issuesAndParsesSubject() {
        JwtService jwtService = new JwtService(new JwtProperties("hotel-os-test-secret-key-32chars!", 3600000));
        UserPrincipal principal = new UserPrincipal(
                1L,
                "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "customer@hotel.com",
                "hash",
                UserRole.customer,
                true
        );

        String token = jwtService.issueToken(principal);
        assertEquals(principal.getPublicId(), jwtService.parseSubject(token));
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(new JwtProperties("hotel-os-test-secret-key-32chars!", 3600000));
        UserPrincipal principal = new UserPrincipal(
                1L,
                "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "customer@hotel.com",
                "hash",
                UserRole.admin,
                true
        );
        String token = jwtService.issueToken(principal) + "x";
        assertThrows(Exception.class, () -> jwtService.parseSubject(token));
    }
}
