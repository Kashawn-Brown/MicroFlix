package com.microflix.userservice.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.microflix.userservice.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Creates HS256 JWT tokens. Secret in application.yml.
 */
public class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final long minutesToExpire;
    private final JWTVerifier verifier;

    public JwtService(String secret, String issuer, long minutesToExpire) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.minutesToExpire = minutesToExpire;

        // Build a verifier that enforces the same issuer and HMAC secret.
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(minutesToExpire, ChronoUnit.MINUTES);

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getEmail())
                .withClaim("userId", user.getId().toString())
                .withClaim("roles", user.getRoles())
                .withIssuedAt(now)
                .withExpiresAt(expiry)
                .sign(algorithm);
    }


    public DecodedJWT verifyToken(String token) {
        return verifier.verify(token);
    }

}
