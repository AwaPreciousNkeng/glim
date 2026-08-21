package com.codewithpcodes.glimserver.auth;

import com.codewithpcodes.glimserver.auth.dtos.*;
import com.codewithpcodes.glimserver.config.JwtService;
import com.codewithpcodes.glimserver.exceptions.*;
import com.codewithpcodes.glimserver.notification.email.VerificationService;
import com.codewithpcodes.glimserver.token.Token;
import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.token.TokenType;
import com.codewithpcodes.glimserver.user.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION = 15;
    private final VerificationService verificationService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            log.error("Email already exists with email::{}", request.email());
            throw new DuplicateResourceException("User already exists.");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.MEMBER)
                .language(request.preferredLanguage() == null ? Language.ENGLISH : request.preferredLanguage())
                .build();
        userRepository.save(user);

        verificationService.sendVerificationEmail(user);
        log.info("Successfully registered user {}", user.getEmail());
        return issueTokens(user);
    }

    public AuthenticationResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password."));
        checkLockOut(user);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (Exception e) {
            handleFailedAttempts(user);
            int remainingAttempts = MAX_ATTEMPTS - user.getFailedLoginAttempts();

            if (remainingAttempts <= 0) {
                throw new ForbiddenException("Account locked due to too many failed attempts. " +
                        "Try again in " + LOCK_DURATION + " minutes."
                );
            }
            log.error("Authentication failed: {}", e.getMessage());
            throw new BadRequestException(
                    "Invalid email or password. " +
                            remainingAttempts + " attempt(s) remaining."
            );
        }
        resetFailedAttempts(user);
        log.info("User {} logged in successfully", user.getEmail());
        return issueTokens(user);
    }

    private AuthenticationResponse issueTokens(User user) {
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);


        return AuthenticationResponse.fromAuth(
                accessToken,
                refreshToken,
                UserResponse.from(user)
        );
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing refresh token");
        }

        String refreshToken = authHeader.substring(7);

        String userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        var accessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        return AuthenticationResponse.fromAuth(accessToken, refreshToken, UserResponse.from(user));
    }

    public AuthenticationResponse createAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User already exists.");
        }

        User admin = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);

        return issueTokens(admin);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .ifPresent(verificationService::sendResetCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCodeException("That code is not correct."));

        verificationService.consumeResetCode(user, request.code());

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        revokeAllUserTokens(user);
    }

    @Transactional
    public void changeEmail(UUID userId, ChangeEmailRequest request) {
        User user = userRepository.findById(userId).orElseThrow();

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password.");
        }

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new DuplicateResourceException("Email already in use.");
        }

        user.setEmail(request.newEmail());
        user.setEmailVerifiedAt(null);
        verificationService.sendVerificationEmail(user);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty()) return;
        validUserTokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    private void saveUserToken(User user, String accessToken) {
        Token token = Token.builder()
                .user(user)
                .token(accessToken)
                .type(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void checkLockOut(User user) {
        if (!user.isAccountLocked()) return;

        if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
            resetFailedAttempts(user);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String unlockTime = user.getLockedUntil() != null ? user.getLockedUntil().format(fmt) : "Soon";
        throw new IllegalArgumentException("Account Locked due to too many failed attempts. " +
                "Try again after " + unlockTime);
    }

    private void handleFailedAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION));
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}

