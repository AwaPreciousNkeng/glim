package com.codewithpcodes.glimserver.auth;

import com.codewithpcodes.glimserver.auth.dtos.LoginRequest;
import com.codewithpcodes.glimserver.auth.dtos.AuthenticationResponse;
import com.codewithpcodes.glimserver.auth.dtos.RegisterRequest;
import com.codewithpcodes.glimserver.config.JwtService;
import com.codewithpcodes.glimserver.exceptions.*;
import com.codewithpcodes.glimserver.token.Token;
import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.token.TokenType;
import com.codewithpcodes.glimserver.user.Role;
import com.codewithpcodes.glimserver.user.User;
import com.codewithpcodes.glimserver.user.UserRepository;
import com.codewithpcodes.glimserver.user.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        String defaultProfilePicture = "https://ui-avatars.com/api?name=" +
                URLEncoder.encode(request.firstName() + " " + request.lastName(), StandardCharsets.UTF_8) +
                "&background=random&color=fff&size=256";

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
                .avatarKey(defaultProfilePicture)
                .build();
        userRepository.save(user);
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
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);

        log.info("User {} logged in successfully", user.getEmail());
        return AuthenticationResponse.fromAuth(
                accessToken,
                refreshToken,
                UserResponse.from(user)
        );
    }

    public AuthenticationResponse refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String refreshToken = getRefreshToken(request, authHeader);

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

    private static @NonNull String getRefreshToken(HttpServletRequest request, String authHeader) {
        String refreshToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("refresh_token")) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {
            throw new UnauthorizedException("Invalid refresh token.");
        }
        return refreshToken;
    }

    public AuthenticationResponse createAdmin(RegisterRequest request) {
        String defaultProfilePicture = "https://ui-avatars.com/api?name=" +
                URLEncoder.encode(request.firstName() + " " + request.lastName(), StandardCharsets.UTF_8) +
                "&background=random&color=fff&size=256";

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User already exists.");
        }

        User admin = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .profilePictureUrl(defaultProfilePicture)
                .role(Role.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);

        String accessToken = jwtService.generateToken(savedAdmin);
        String refreshToken = jwtService.generateRefreshToken(savedAdmin);

        saveUserToken(savedAdmin, accessToken);
        return AuthenticationResponse.fromAuth(
                accessToken,
                refreshToken,
                UserResponse.from(savedAdmin)
        );
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

