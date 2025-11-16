package com.microflix.userservice.auth;

import com.microflix.userservice.auth.dto.AuthResponse;
import com.microflix.userservice.auth.dto.LoginRequest;
import com.microflix.userservice.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    void register_returnsAuthResponseWithTokenAndRequestData() {
        // arrange: create a RegisterRequest
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "Test User"
        );
        // arrange: create an AuthResponse to expect back from the service
        AuthResponse response = new AuthResponse(
                "register-token",
                "test@example.com",
                "Test User",
                "USER"
        );

        // stub authService.register(request) to return the AuthResponse
        when(authService.register(request)).thenReturn(response);

        // act: call controller.register(request)
        ResponseEntity<AuthResponse> responseEntity = controller.register(request);

        // assert: controller returned what service returns
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());                    // assert: Status is 200 OK
        AuthResponse body = responseEntity.getBody();
        assertNotNull(body);                                                            // assert: Body is non-null

        // assert: Body fields (token, email, displayName, roles) match expectations
        assertEquals(response.token(), body.token());
        assertEquals(response.email(), body.email());
        assertEquals(response.displayName(), body.displayName());
        assertEquals(response.roles(), body.roles());

        // verify the service was called with the same request
        verify(authService).register(eq(request));
    }

    @Test
    void login_returnsAuthResponseWithTokenAndEmail() {
        // arrange: create a LoginRequest
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        // arrange: create an AuthResponse to expect back from the service
        AuthResponse response = new AuthResponse(
                "login-token",
                "test@example.com",
                "Test User",
                "USER"
        );

        // stub authService.login(request) to return the AuthResponse
        when(authService.login(eq(request))).thenReturn(response);

        // act: call controller.login(request)
        ResponseEntity<AuthResponse> responseEntity = controller.login(request);

        // assert
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());                    // assert: Status is 200 OK
        AuthResponse body = responseEntity.getBody();
        assertNotNull(body);                                                            // assert: Body is non-null

        // assert: Body fields (token, email, displayName, roles) match expectations
        assertEquals("login-token", body.token());
        assertEquals("test@example.com", body.email());
        assertEquals("Test User", body.displayName());
        assertEquals("USER", body.roles());

        // verify the service was called with the same request
        verify(authService).login(eq(request));
    }
}
