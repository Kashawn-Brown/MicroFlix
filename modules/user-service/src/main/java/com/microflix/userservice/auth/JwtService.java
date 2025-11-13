package com.microflix.userservice.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Creates HS256 JWT tokens. Secret in application.yml.
 */
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final long minutesToExpire;

    public JwtService(String secret, String issuer, long minutesToExpire) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.minutesToExpire = minutesToExpire;
    }

    public String createToken(String email, String roles) {
        Instant now = Instant.now();

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(email)
                .withClaim("roles", roles)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(minutesToExpire, ChronoUnit.MINUTES))
                .sign(algorithm);
    }

}
