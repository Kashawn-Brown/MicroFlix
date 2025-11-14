package com.microflix.userservice.auth;

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
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public String register(RegisterRequest request) {
        if (users.existsByEmail(request.email())){
            throw new IllegalArgumentException("Email already in use");     // enforce unique emails
        }

        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(request.email().toLowerCase());
        u.setPasswordHash(encoder.encode(request.password()));          // Hash password
        u.setDisplayName(request.displayName());
        u.setRoles("USER");
        u.setActive(true);
        u.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        u.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Save user to DB
        users.save(u);

        // Generate JWT
        return jwt.createToken(u.getEmail(), u.getRoles());
    }

    public String login(LoginRequest request) {
        User u = users.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!encoder.matches(request.password(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Update last login
        u.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Save user to DB
        users.save(u);

        // Generate JWT
        return jwt.createToken(u.getEmail(), u.getRoles());
    }


}
