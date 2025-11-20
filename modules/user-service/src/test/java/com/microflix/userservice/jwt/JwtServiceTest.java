package com.microflix.userservice.jwt;

import com.microflix.userservice.auth.JwtService;
import com.microflix.userservice.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void generateToken_includesUserIdClaim() {
        // arrange
        JwtService jwtService = new JwtService(
                "secret-secret-secret-key",    // match app.jwt.secret
                "microflix-user-service",      // match app.jwt.issuer
                60                              // ttl-minutes or whatever your ctor expects
        );

        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed");       // not important for this test
        user.setRoles("USER");                // or however you store roles

        // act
        String token = jwtService.createToken(user);

        // decode token to inspect claims
        var decoded = jwtService.verifyToken(token);   // if you have a helper
        // if not, you can use JWT.decode(token) directly from the library you use

        // assert
        assertEquals("test@example.com", decoded.getSubject());
        assertEquals(userId.toString(), decoded.getClaim("userId").asString());
        String roles = decoded.getClaim("roles").asString();
        assertTrue(roles.contains("USER"));
    }
}
