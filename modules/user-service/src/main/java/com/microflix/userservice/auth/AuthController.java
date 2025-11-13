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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Validated @RequestBody RegisterRequest request) {
        String token = service.register(request);

        AuthResponse response = new AuthResponse(token, request.email(), request.displayName(),"USER");

        return ResponseEntity.ok(response);
    }

    @PostMapping("login")
    ResponseEntity<AuthResponse> login(@Validated @RequestBody LoginRequest request) {
        String token = service.login(request);

        AuthResponse response = new AuthResponse(token, request.email(), null, "USER");

        return ResponseEntity.ok(response);
    }

}
