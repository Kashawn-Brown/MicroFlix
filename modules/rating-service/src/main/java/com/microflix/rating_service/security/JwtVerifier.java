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

// Small helper responsible only for verifying tokens and building a CurrentUser.
@Component
public class JwtVerifier {

    private final JWTVerifier verifier;

    public JwtVerifier(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer
    ) {

        // HMAC with a shared secret (matches user-service)
        Algorithm algorithm = Algorithm.HMAC256(secret);


        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public CurrentUser verify(String token) {

        // Verify signature + issuer and parse claims
        DecodedJWT jwt = verifier.verify(token);

        // retrieve user id from token
        String userIdString = jwt.getClaim("userId").asString();
        UUID userId = UUID.fromString(userIdString);

        String email = jwt.getSubject(); // 'sub' claim, we use email here

        // roles come as string "USER, ADMIN" convert
        String rolesString = jwt.getClaim("roles").asString();
        List<String> roles = Arrays.stream(rolesString.split(","))                    // drop blanks/empty items
                                    .filter(StringUtils::hasText)                           // remove leading/trailing spaces
                                    .map(String::trim)
                                    .toList();


        return new CurrentUser(userId, email, roles);
    }


}
