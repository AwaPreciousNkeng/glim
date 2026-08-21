package com.codewithpcodes.glimserver.auth;

import com.codewithpcodes.glimserver.auth.dtos.*;
import com.codewithpcodes.glimserver.notification.email.VerificationService;
import com.codewithpcodes.glimserver.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Management", description = "Authentication Management Endpoints")
public class AuthenticationController {

    private final AuthenticationService service;
    private final VerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.register(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        verificationService.consumeVerificationToken(token);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(verifiedPage());
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgot(@RequestBody @Valid ForgotPasswordRequest request) {
        service.forgotPassword(request);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@RequestBody @Valid ResetPasswordRequest request) {
        service.resetPassword(request);
    }

    @PostMapping("/email/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(@AuthenticationPrincipal User user, ChangeEmailRequest request) {
        service.changeEmail(user.getId(), request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public ResponseEntity<AuthenticationResponse> createAdmin(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAdmin(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(service.refreshToken(request));
    }
}
