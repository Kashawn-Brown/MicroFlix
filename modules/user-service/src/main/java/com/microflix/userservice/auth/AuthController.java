package com.microflix.userservice.auth;

import com.microflix.userservice.auth.dto.AuthResponse;
import com.microflix.userservice.auth.dto.LoginRequest;
import com.microflix.userservice.auth.dto.RegisterRequest;
import org.aspectj.weaver.patterns.IToken;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints -> register and login.
 *
 * Routes here are public (SecurityConfig):
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * User Register
     *
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Validated @RequestBody RegisterRequest request) {
        var response = service.register(request);

        return ResponseEntity.ok(response);
    }


    /**
     * User Login
     *
     */
    @PostMapping("login")
    ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
        var response = service.login(request);

        return ResponseEntity.ok(response);
    }

}
