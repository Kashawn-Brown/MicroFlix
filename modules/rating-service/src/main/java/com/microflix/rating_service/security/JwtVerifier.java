package com.microflix.rating_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Responsible for verifying JWT tokens and converting them into a CurrentUser.
@Component
public class JwtVerifier {

    private final JWTVerifier verifier;

    public JwtVerifier(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer
    ) {

        // Use HMAC with a shared secret; matches the issuer service.
        Algorithm algorithm = Algorithm.HMAC256(secret);

        // Pre-build a verifier that checks signature + issuer.
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }


    /**
     * Verifies the given JWT + returns authenticated user data
     *
     * Checks its signature and issuer
     * Extracts the authenticated user's data from its claims.
     */
    public CurrentUser verify(String token) {

        // Verify signature + issuer and parse claims
        DecodedJWT jwt = verifier.verify(token);

        // Extract user ID from "userId" claim.
        String userIdString = jwt.getClaim("userId").asString();
        UUID userId = UUID.fromString(userIdString);

        // Email is stored in the subject ("sub").
        String email = jwt.getSubject();

        // Roles come as a comma-separated string, e.g. "USER,ADMIN".
        String rolesString = jwt.getClaim("roles").asString();
        List<String> roles = Arrays.stream(rolesString.split(","))
                                    .filter(StringUtils::hasText)   // drop blanks/empty items
                                    .map(String::trim)  // remove leading/trailing spaces
                                    .toList();

        // Build our domain model for the authenticated user.
        return new CurrentUser(userId, email, roles);
    }


}
