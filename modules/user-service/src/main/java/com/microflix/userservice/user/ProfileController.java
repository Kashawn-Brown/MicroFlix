package com.microflix.userservice.user;

import com.microflix.userservice.security.CurrentUser;
import com.microflix.userservice.user.dto.ChangePasswordRequest;
import com.microflix.userservice.user.dto.ProfileMeResponse;
import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * Profile endpoints for the currently authenticated user.
 */
@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }





    /**
     * Returns profile info for the current user.
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileMeResponse> me(Authentication auth) {
        // Spring injects Authentication from SecurityContext (set by our JwtAuthFilter)
        // Authentication is a recognized parameter type -> reads the Authentication from SecurityContextHolder.getContext() and passes it in

        var currentUser = CurrentUser.set(auth);

        var response = profileService.me(currentUser);

        return ResponseEntity.ok(response);

    }


    /**
     * Updates the current user's profile
     *
     * Basic for now -> only update displayName
     */
    @PatchMapping("/me")
    public ResponseEntity<ProfileMeResponse> updateProfile(
            @Validated @RequestBody UpdateProfileRequest request,
            Authentication auth
    ) {

        var currentUser = CurrentUser.set(auth);

        var response = profileService.updateProfile(currentUser, request);

        return ResponseEntity.ok(response);
    }


    /**
     * Change the current user's password.
     *
     * Returns 204 on success.
     */
    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @Validated @RequestBody ChangePasswordRequest request,
            Authentication auth
    ) {

        var currentUser = CurrentUser.set(auth);

        profileService.changePassword(currentUser, request);

        // 204 No Content — change succeeded, nothing else to return
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
