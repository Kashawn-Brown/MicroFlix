package com.microflix.userservice.user;

import com.microflix.userservice.security.CurrentUser;
import com.microflix.userservice.user.dto.ChangePasswordRequest;
import com.microflix.userservice.user.dto.ProfileMeResponse;
import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Profile endpoints -> For current user endpoints
 *
 * Protected sample endpoint:
 * - requires a valid JWT (per security rules)
 * - returns the current user's email + roles
 */
@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**     ROUTES      **/

    // Spring injects Authentication from SecurityContext (set by our JwtAuthFilter)
    @GetMapping("/me")
    public ResponseEntity<ProfileMeResponse> me(Authentication auth) { // Authentication is a recognized parameter type -> reads the Authentication from SecurityContextHolder.getContext() and passes it in

        var currentUser = CurrentUser.set(auth);

        var response = profileService.me(currentUser);

        return ResponseEntity.ok(response);

    }


    /**
     * Update User
     *
     * Basic for now -> only update displayName
     * **/
    @PatchMapping("/me")
    public ResponseEntity<ProfileMeResponse> updateProfile(@Validated @RequestBody UpdateProfileRequest request, Authentication auth) {

        var currentUser = CurrentUser.set(auth);

        var response = profileService.updateProfile(currentUser, request);

        return ResponseEntity.ok(response);
    }


    /**
     * Change the current user's password.
     *
     * - validates old password
     * - sets new password
     */
    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(@Validated @RequestBody ChangePasswordRequest request, Authentication auth) {

        var currentUser = CurrentUser.set(auth);

        profileService.changePassword(currentUser, request);

        // 204 No Content — change succeeded, nothing else to return
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
