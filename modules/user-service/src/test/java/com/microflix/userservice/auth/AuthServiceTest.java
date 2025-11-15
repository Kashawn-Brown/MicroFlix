package com.microflix.userservice.auth;

import com.microflix.userservice.auth.dto.AuthResponse;
import com.microflix.userservice.auth.dto.LoginRequest;
import com.microflix.userservice.auth.dto.RegisterRequest;
import com.microflix.userservice.user.User;
import com.microflix.userservice.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository users;

    @Mock
    PasswordEncoder encoder;

    @Mock
    JwtService jwt;

    @InjectMocks
    AuthService authService;

    @Test
    void register_createsUser_andReturnsAuthResponse() {
        // arrange
        var request = new RegisterRequest("Test@Example.com", "password123", "Test User");

        when(users.existsByEmail("test@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed-password");

        // simulate JPA assigning an id + default fields (adjust if your entity differs)
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            if (u.getRoles() == null) {
                u.setRoles("USER");
            }
            if (u.getCreatedAt() == null) {
                var now = OffsetDateTime.now(ZoneOffset.UTC);
                u.setCreatedAt(now);
                u.setUpdatedAt(now);
            }
            return u;
        });

        when(jwt.createToken("test@example.com", "USER")).thenReturn("fake-jwt-token");

        // act
        AuthResponse response = authService.register(request);

        // assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.token());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.displayName());
        assertEquals("USER", response.roles());

        // verify user persisted with normalized email & hashed password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("test@example.com", saved.getEmail()); // lower-cased
        assertEquals("hashed-password", saved.getPasswordHash());
        assertEquals("Test User", saved.getDisplayName());
        assertEquals("USER", saved.getRoles());
    }

    @Test
    void register_withExistingEmail_throwsIllegalArgumentException() {
        // arrange
        var request = new RegisterRequest("user@example.com", "password123", "User");
        when(users.existsByEmail("user@example.com")).thenReturn(true);

        // act + assert
        var ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Email is already registered", ex.getMessage());
        verify(users, never()).save(any());
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        // arrange
        var request = new LoginRequest("user@example.com", "password123");

        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("user@example.com");
        existing.setPasswordHash("hashed-password");
        existing.setDisplayName("Existing User");
        existing.setRoles("USER");

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(encoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwt.createToken("user@example.com", "USER")).thenReturn("login-jwt-token");

        // act
        AuthResponse response = authService.login(request);

        // assert
        assertNotNull(response);
        assertEquals("login-jwt-token", response.token());
        assertEquals("user@example.com", response.email());
        assertEquals("Existing User", response.displayName());
        assertEquals("USER", response.roles());

        // lastLoginAt should have been updated and saved
        verify(users).save(existing);
        assertNotNull(existing.getLastLoginAt());
    }

    @Test
    void login_withInvalidPassword_throwsIllegalArgumentException() {
        // arrange
        var request = new LoginRequest("user@example.com", "wrong-password");

        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("user@example.com");
        existing.setPasswordHash("hashed-password");
        existing.setDisplayName("Existing User");
        existing.setRoles("USER");

        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(encoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // act + assert
        var ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
        verify(users, never()).save(existing);
        verify(jwt, never()).createToken(anyString(), anyString());
    }
}
