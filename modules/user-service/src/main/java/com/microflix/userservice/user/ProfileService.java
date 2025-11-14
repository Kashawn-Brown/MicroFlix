package com.microflix.userservice.user;

import com.microflix.userservice.security.CurrentUser;
import com.microflix.userservice.user.dto.ChangePasswordRequest;
import com.microflix.userservice.user.dto.ProfileMeResponse;
import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class ProfileService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public ProfileService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /**
     * "me" response for the current user.
     *
     * Uses DTO instead of raw Map for a stable, typed contract.
     */
    public ProfileMeResponse me(CurrentUser currentUser) {
        // find current user by email
        var user = users.findByEmail(currentUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new ProfileMeResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),  currentUser.roles()
        );
    }

    /**
     * Update the current user's profile.
     *
     * For now, only displayName is supported.
     */
    public ProfileMeResponse update(CurrentUser currentUser, UpdateProfileRequest request) {

        // find current user by email
        var user = users.findByEmail(currentUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // update displayName
        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }

        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        users.save(user);

        // minimal response —> a snapshot after update
        return new ProfileMeResponse(
                user.getId(), user.getEmail(), user.getDisplayName(),  currentUser.roles()
        );
    }

    /**
     * Change the current user's password.
     *
     * Throws IllegalArgumentException if the old password is wrong,
     * which our ErrorAdvice will convert to a 400.
     */
    public ResponseEntity<?> changePassword(CurrentUser currentUser, ChangePasswordRequest request) {

        // get current user by email
        var user = users.findByEmail(currentUser.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // verify current password matches stored hash
        if (!encoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        // hash and set the new password
        user.setPasswordHash(encoder.encode(request.newPassword()));

        // update audit timestamp
        user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        users.save(user);

        // 204 No Content — change succeeded, nothing else to return
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
