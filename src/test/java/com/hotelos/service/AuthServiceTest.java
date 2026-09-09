package com.hotelos.service;

import com.hotelos.domain.User;
import com.hotelos.domain.enums.UserRole;
import com.hotelos.repository.UserRepository;
import com.hotelos.security.JwtService;
import com.hotelos.security.UserPrincipal;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.AuthDtos.LoginRequest;
import com.hotelos.web.dto.AuthDtos.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void customerLoginIssuesJwtForCustomers() {
        User user = user(UserRole.customer);
        stubPasswordLogin(user);

        LoginResponse response = authService.customerLogin(new LoginRequest("customer@hotel.com", "customer123"));

        assertEquals("jwt-token", response.token());
        assertEquals(UserRole.customer, response.user().role());
    }

    @Test
    void customerLoginRejectsStaff() {
        stubPasswordLogin(user(UserRole.manager));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> authService.customerLogin(new LoginRequest("manager@hotel.com", "manager123"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void controlledLoginIssuesJwtForManagerAndAdmin() {
        User user = user(UserRole.admin);
        stubPasswordLogin(user);

        LoginResponse response = authService.controlledLogin(new LoginRequest("admin@hotel.com", "admin123"));

        assertEquals("jwt-token", response.token());
        assertEquals(UserRole.admin, response.user().role());
    }

    @Test
    void controlledLoginRejectsCustomers() {
        stubPasswordLogin(user(UserRole.customer));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> authService.controlledLogin(new LoginRequest("customer@hotel.com", "customer123"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void oauth2CreatesCustomerWhenEmailIsNew() {
        when(userRepository.findByEmail("guest@hotel.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("oauth-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issueToken(any(UserPrincipal.class))).thenReturn("jwt-token");
        when(jwtService.expirationMs()).thenReturn(1000L);

        LoginResponse response = authService.completeCustomerOAuth2("guest@hotel.com", "Guest User");

        assertEquals(UserRole.customer, response.user().role());
        assertEquals("guest@hotel.com", response.user().email());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.customer, captor.getValue().getRole());
    }

    @Test
    void oauth2RejectsStaffAccounts() {
        when(userRepository.findByEmail("admin@hotel.com")).thenReturn(Optional.of(user(UserRole.admin)));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> authService.completeCustomerOAuth2("admin@hotel.com", "Priya Sharma")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    private void stubPasswordLogin(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));
        when(jwtService.issueToken(principal)).thenReturn("jwt-token");
        when(jwtService.expirationMs()).thenReturn(1000L);
    }

    private static User user(UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setPublicId("01ARZ3NDEKTSV4RRFFQ69G5FAV");
        user.setFullName("Test User");
        user.setEmail(role.name() + "@hotel.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
