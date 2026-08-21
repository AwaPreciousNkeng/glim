package com.codewithpcodes.glimserver.config;

import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final TokenRepository tokenRepository;
    @Value("${glim.jwt.secret}")
    private String secretKey;

    @Value("${glim.jwt.expiration}")
    private long jwtExpiration;

    @Value("${glim.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public JwtService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e1) {
            try {
                keyBytes = Decoders.BASE64URL.decode(secretKey);
            } catch (Exception e2) {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
        }

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        User user = (User) userDetails;
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long jwtExpiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpired() {
        int removed = tokenRepository.deleteExpiredBefore(Instant.now().minus(30, ChronoUnit.DAYS));
        if (removed > 0) log.info("Purged {} expired refresh tokens", removed);
    }
}
