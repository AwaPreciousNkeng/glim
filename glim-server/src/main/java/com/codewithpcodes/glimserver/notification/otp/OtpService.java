package com.codewithpcodes.glimserver.notification.otp;

import com.codewithpcodes.glimserver.exceptions.InvalidCodeException;
import com.codewithpcodes.glimserver.exceptions.TooManyRequestsException;
import com.codewithpcodes.glimserver.notification.NotificationDispatcher;
import com.codewithpcodes.glimserver.notification.NotificationService;
import com.codewithpcodes.glimserver.notification.NotificationType;
import com.codewithpcodes.glimserver.user.CodePurpose;
import com.codewithpcodes.glimserver.user.User;
import com.codewithpcodes.glimserver.user.VerificationCode;
import com.codewithpcodes.glimserver.user.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class OtpService {

    private final VerificationCodeRepository codeRepository;
    private final NotificationService notificationService;
    private final AuthProperties props;
    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------
    // ISSUE
    // ------------------------------------------------------------------

    @Transactional
    public void issue(User user, CodePurpose purpose) {

        // Hourly cap — stops someone burning your Twilio credit.
        long recent = codeRepository.countRecent(
                user.getId(), purpose, Instant.now().minus(1, ChronoUnit.HOURS));

        if (recent >= props.getOtpMaxResendsPerHour()) {
            throw new TooManyRequestsException("Too many codes requested. Try again later.");
        }

        // Cooldown, and invalidate the previous code so only one is ever live.
        codeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .ifPresent(existing -> {
                    Instant earliest = existing.getCreatedAt().plus(props.getOtpResendCooldown());
                    if (Instant.now().isBefore(earliest)) {
                        throw new TooManyRequestsException("Please wait before requesting another code.");
                    }
                    existing.setConsumedAt(Instant.now());
                });

        String plainCode = randomDigits(props.getOtpLength());

        codeRepository.save(VerificationCode.builder()
                .user(user)
                .codeHash(sha256(plainCode))
                .purpose(purpose)
                .expiresAt(Instant.now().plus(props.getOtpTtl()))
                .build());

        deliver(user, purpose, plainCode);
    }

    // ------------------------------------------------------------------
    // VERIFY
    // ------------------------------------------------------------------

    @Transactional
    public void verify(User user, CodePurpose purpose, String submitted) {

        VerificationCode code = codeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new InvalidCodeException("No active code. Request a new one."));

        if (!code.isUsable()) {
            throw new InvalidCodeException("This code has expired. Request a new one.");
        }

        if (code.getAttempts() >= props.getOtpMaxAttempts()) {
            code.setConsumedAt(Instant.now());
            throw new InvalidCodeException("Too many incorrect attempts. Request a new code.");
        }

        // Increment BEFORE comparing — otherwise a thrown exception could
        // roll back the counter and give unlimited guesses.
        code.setAttempts(code.getAttempts() + 1);

        if (!constantTimeEquals(sha256(submitted), code.getCodeHash())) {
            throw new InvalidCodeException("That code is not correct.");
        }

        code.setConsumedAt(Instant.now());
    }

    // ------------------------------------------------------------------
    // DELIVERY
    // ------------------------------------------------------------------

    private void deliver(User user, CodePurpose purpose, String code) {

        var recipient = new NotificationDispatcher.Recipient(
                user.getId(), user.getLanguage().name(), user.getPhoneNumber()
        );

        NotificationType type = switch (purpose) {
            case REGISTRATION, NEW_DEVICE -> NotificationType.OTP_REGISTRATION;
            case PASSWORD_RESET -> NotificationType.OTP_PASSWORD_RESET;
        };

        // BLOCKING by declaration — a failure here rolls back the stored code,
        // which is correct: a code nobody received is worse than a visible error.
        notificationService.notify(recipient, type, null, code);
    }

    // ------------------------------------------------------------------
    // CLEANUP
    // ------------------------------------------------------------------

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeSpentCodes() {
        int removed = codeRepository.deleteSpentBefore(Instant.now().minus(7, ChronoUnit.DAYS));
        if (removed > 0) log.info("Purged {} spent verification codes", removed);
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Prevents timing attacks from revealing how much of the hash matched. */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}