package com.microflix.userservice.auth;

import com.microflix.userservice.auth.dto.AuthResponse;
import com.microflix.userservice.auth.dto.LoginRequest;
import com.microflix.userservice.auth.dto.RegisterRequest;
import com.microflix.userservice.user.User;
import com.microflix.userservice.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AuthService {      // Handles registration and login logic.

    private final UserRepository users;

    // Auto-injects Beans from security config
    private final PasswordEncoder encoder;      // injected bean for hashing/verifying passwords
    private final JwtService jwt;               // injected bean for issuing and validating JWTs

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /**
     * Register a new user
     *
     * - Ensures email is unique
     * - Hashes password
     * - Saves user
     * - Generates JWT for immediate login
     */
    public AuthResponse register(RegisterRequest request) {
        var normalizedEmail = request.email().toLowerCase();

        if (users.existsByEmail(normalizedEmail)){
            throw new IllegalArgumentException("Email is already registered");     // enforce unique emails
        }

        var user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName());

        // Save user to DB
        users.save(user);

        // Generate JWT
        var token = jwt.createToken(user);

        return new AuthResponse(
                token, user.getEmail(), user.getDisplayName(), user.getRoles()
        );

    }

    /**
     * Login existing user
     *
     * - Validates email/password
     * - Updates lastLoginAt
     * - Generates and returns a new JWT
     */
    public AuthResponse login(LoginRequest request) {
        var normalizedEmail = request.email().toLowerCase();

        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // If user is deactivated
        // if (!user.isActive()) {
        //     throw new IllegalArgumentException("Account is inactive");
        // }

        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Update last login
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Save user to DB
        users.save(user);

        // Generate JWT
        var token = jwt.createToken(user);

        return new AuthResponse(
                token, user.getEmail(), user.getDisplayName(), user.getRoles()
        );
    }


}
