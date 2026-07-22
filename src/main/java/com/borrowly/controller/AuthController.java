package com.borrowly.controller;

import com.borrowly.dto.request.LoginRequest;
import com.borrowly.dto.request.RegisterRequest;
import com.borrowly.dto.response.AuthResponse;
import com.borrowly.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String JWT_COOKIE_NAME = "jwt_token";

    private final AuthService authService;

    @Value("${borrowly.security.jwt-cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${borrowly.security.jwt-cookie.max-age-seconds:86400}")
    private long cookieMaxAgeSeconds;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createJwtCookie(response.token()).toString())
                .body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createJwtCookie(response.token()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expireJwtCookie().toString())
                .build();
    }

    private ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)                             // not readable from JS (anti-XSS)
                .secure(cookieSecure)                       // true in prod (HTTPS only)
                .path("/")                                  // valid site-wide
                .maxAge(Duration.ofSeconds(cookieMaxAgeSeconds))
                .sameSite("Strict")                         // anti-CSRF
                .build();
    }

    private ResponseCookie expireJwtCookie() {
        return ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}