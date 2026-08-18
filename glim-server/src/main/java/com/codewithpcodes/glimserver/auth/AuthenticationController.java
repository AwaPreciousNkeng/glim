package com.codewithpcodes.glimserver.auth;

import com.codewithpcodes.harmoniq.user.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication Management Endpoints")
public class AuthenticationController {

    private final AuthenticationService service;

    @Value("${application.production}")
    private boolean production;

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        service.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<UserResponse> createAdmin(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthenticationResponse authResponse = service.createAdmin(request);
        return getUserResponse(authResponse);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        AuthenticationResponse authResponse = service.authenticate(request);
        return getUserResponse(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        AuthenticationResponse authResponse = service.refreshToken(request, response);
        return getUserResponse(authResponse);
    }

    @NonNull
    private ResponseEntity<UserResponse> getUserResponse(AuthenticationResponse authResponse) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", authResponse.accessToken())
                .httpOnly(true)
                .secure(production)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", authResponse.refreshToken())
                .httpOnly(true)
                .secure(production)
                .path("/api/v1/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponse.user());
    }
}
