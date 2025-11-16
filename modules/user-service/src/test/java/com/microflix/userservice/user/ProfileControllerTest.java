package com.microflix.userservice.user;

import com.microflix.userservice.auth.AuthService;
import com.microflix.userservice.auth.dto.AuthResponse;
import com.microflix.userservice.security.CurrentUser;
import com.microflix.userservice.user.dto.ChangePasswordRequest;
import com.microflix.userservice.user.dto.ProfileMeResponse;
import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController controller;

    @Test
    void me_returnsEmailAndRolesFromAuthentication() {
        // arrange: create real Authentication with ROLE_USER
        var auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // arrange: create what the service should return
        UUID id = UUID.randomUUID();
        ProfileMeResponse response = new ProfileMeResponse(
                id,
                "user@example.com",
                "Test User",
                List.of("ROLE_USER")
        );

        // stub profileService.register(request) to return the ProfileMeResponse
        when(profileService.me(any(CurrentUser.class))).thenReturn(response);

        // act: call controller.me(auth)
        ResponseEntity<ProfileMeResponse> responseEntity = controller.me(auth);

        // assert: controller returned what service returns
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());                    // assert: Status is 200 OK
        ProfileMeResponse body = responseEntity.getBody();
        assertNotNull(body);                                                            // assert: Body is non-null

        // assert: Body fields (token, email, displayName, roles) match expectations
        assertEquals(response.id(), body.id());
        assertEquals(response.email(), body.email());
        assertEquals(response.displayName(), body.displayName());
        assertEquals(response.roles(), body.roles());

        // verify the service was called with a CurrentUser instance
        verify(profileService).me(any(CurrentUser.class));
    }

    @Test
    void update_updatesDisplayNameAndReturnsSnapshot() {
        // arrange: create real Authentication with ROLE_USER
        var auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // arrange: create a UpdateProfileRequest
        UpdateProfileRequest request = new UpdateProfileRequest("New Name");

        // arrange: create what the service should return
        UUID id = UUID.randomUUID();
        ProfileMeResponse response = new ProfileMeResponse(
                id,
                "user@example.com",
                "New Name",                             // Name change
                List.of("ROLE_USER")
        );

        // stub profileService.updateProfile(currentUser, request) to return the ProfileMeResponse
        when(profileService.updateProfile(any(CurrentUser.class), eq(request))).thenReturn(response);

        // act: call controller.updateProfile(request, auth)
        ResponseEntity<ProfileMeResponse> responseEntity = controller.updateProfile(request, auth);

        // assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());                    // assert: Status is 200 OK
        ProfileMeResponse body = responseEntity.getBody();
        assertNotNull(body);                                                            // assert: Body is non-null

        // assert: user entity updated
        assertEquals("New Name", body.displayName());                    // Only one we care about (for now)


        // assert: jus checking the rest of the response matches
        assertEquals(response.id(), body.id());
        assertEquals(response.email(), body.email());
        assertEquals(response.displayName(), body.displayName());
        assertEquals(response.roles(), body.roles());

        // verify the service was called with a CurrentUser instance
        verify(profileService).updateProfile(any(CurrentUser.class), eq(request));

    }

    @Test
    void changePassword_withCorrectOldPassword_updatesHashAndReturns204() {
        // arrange: create real Authentication with ROLE_USER
        var auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // arrange: create a ChangePasswordRequest
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "old-hash");

        // stub profileService.changePassword(currentUser, request) to return the empty response
        when(profileService.changePassword(any(CurrentUser.class), eq(request))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // act: call controller.changePassword(request, auth)
        ResponseEntity<?> responseEntity = controller.changePassword(request, auth);

        // assert
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());                    // assert: Status is 204 NO CONTENT
        var body = responseEntity.getBody();
        assertNull(body);

    }

    @Test
    void changePassword_withWrongOldPassword_throwsIllegalArgumentException() {
        // arrange: create real Authentication with ROLE_USER
        var auth = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // arrange: create a ChangePasswordRequest
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-pass", "old-hash");

        // stub profileService.changePassword(currentUser, request) to throw an IllegalArgumentException
        when(profileService.changePassword(any(CurrentUser.class), eq(request)))
                .thenThrow(new IllegalArgumentException("Old password is incorrect"));

        // act: call controller.changePassword(request, auth)
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.changePassword(request, auth)
        );

        assertEquals("Old password is incorrect", ex.getMessage());
    }
}
