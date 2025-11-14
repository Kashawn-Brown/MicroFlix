package com.microflix.userservice.user;

import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Profile endpoint -> For current user endpoints
 * Protected sample endpoint:
 * - requires a valid JWT (per security rules)
 * - returns the current user's email + roles
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    // Spring injects Authentication from SecurityContext (set by our JwtAuthFilter)
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {            // Authentication is a recognized parameter type -> reads the Authentication from SecurityContextHolder.getContext() and passes it in
        return Map.of(
                "email", auth.getName(),
                "roles", auth.getAuthorities().stream().map(Object::toString).toList()
        );
    }


    @PatchMapping
    public Map<String, Object> update(@Validated @RequestBody UpdateProfileRequest req, Authentication auth) {

        // find current user by email
        var user = users.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // update displayName
        if (req.displayName() != null) {
            user.setDisplayName(req.displayName());
        }

        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        users.save(user);

        // minimal response —> a snapshot after update
        return Map.of(
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
        );

    }


}
