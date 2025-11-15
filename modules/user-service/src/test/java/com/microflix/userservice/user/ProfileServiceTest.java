package com.microflix.userservice.user;

import com.microflix.userservice.security.CurrentUser;
import com.microflix.userservice.user.dto.ChangePasswordRequest;
import com.microflix.userservice.user.dto.ProfileMeResponse;
import com.microflix.userservice.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    UserRepository users;

    @Mock
    PasswordEncoder encoder;

    @InjectMocks
    ProfileService profileService;

    @Test
    void me_returnsProfileMeResponse_forExistingUser() {
        // arrange
        var email = "user@example.com";
        var roles = List.of("ROLE_USER");
        var currentUser = new CurrentUser(email, roles);

        User entity = new User();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setEmail(email);
        entity.setDisplayName("Test User");
        entity.setRoles("USER"); // internal representation

        when(users.findByEmail(email)).thenReturn(Optional.of(entity));

        // act
        ProfileMeResponse response = profileService.me(currentUser);

        // assert
        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(email, response.email());
        assertEquals("Test User", response.displayName());
        assertEquals(roles, response.roles());

        verify(users).findByEmail(email);
    }

    @Test
    void me_whenUserNotFound_throwsIllegalArgumentException() {
        // arrange
        var email = "missing@example.com";
        var currentUser = new CurrentUser(email, List.of("ROLE_USER"));

        when(users.findByEmail(email)).thenReturn(Optional.empty());

        // act + assert
        var ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.me(currentUser));

        assertEquals("User not found", ex.getMessage());
        verify(users).findByEmail(email);
    }

    @Test
    void changePassword_withCorrectOldPassword_updatesHash_andUpdatedAt() {
        // arrange
        var email = "user@example.com";
        var currentUser = new CurrentUser(email, List.of("ROLE_USER"));

        User entity = new User();
        entity.setId(UUID.randomUUID());
        entity.setEmail(email);
        entity.setPasswordHash("old-hash");

        when(users.findByEmail(email)).thenReturn(Optional.of(entity));
        when(encoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(encoder.encode("new-pass")).thenReturn("new-hash");

        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "new-pass");

        // act
        profileService.changePassword(currentUser, request);

        // assert
        // saved user has updated hash and updatedAt
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("new-hash", saved.getPasswordHash());
        assertNotNull(saved.getUpdatedAt());

        // updatedAt should be "around now" (we just check it's close-ish)
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        assertTrue(
                !saved.getUpdatedAt().isAfter(now.plusSeconds(5)),
                "updatedAt should not be in the far future"
        );
    }

    @Test
    void changePassword_withIncorrectOldPassword_throwsIllegalArgumentException() {
        // arrange
        var email = "user@example.com";
        var currentUser = new CurrentUser(email, List.of("ROLE_USER"));

        User entity = new User();
        entity.setId(UUID.randomUUID());
        entity.setEmail(email);
        entity.setPasswordHash("old-hash");

        when(users.findByEmail(email)).thenReturn(Optional.of(entity));
        when(encoder.matches("wrong-pass", "old-hash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("wrong-pass", "new-pass");

        // act + assert
        var ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.changePassword(currentUser, request));

        assertEquals("Old password is incorrect", ex.getMessage());

        // ensure we didn't save or generate new hash
        verify(users, never()).save(any(User.class));
        verify(encoder, never()).encode("new-pass");
    }
}
