package com.codewithpcodes.glimserver.notification.email;

import com.codewithpcodes.glimserver.auth.util.HashUtil;
import com.codewithpcodes.glimserver.exceptions.InvalidCodeException;
import com.codewithpcodes.glimserver.exceptions.TooManyRequestsException;
import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class VerificationService {

    private final VerificationCodeRepository codeRepository;
    private final SmtpEmailSender emailSender;
    private final MailProperties props;
    private final SecureRandom random = new SecureRandom();
    private final TokenRepository tokenRepository;
    private final SwaggerIndexTransformer indexPageTransformer;
    private final EmailTemplateRenderer emailTemplateRenderer;

    @Async("notificationExecutor")
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.getEmail() == null || user.isEmailVerified()) return;

        if (isRateLimited(user, CodePurpose.VERIFY_EMAIL)) {
            log.info("Verification email rate-limited for user {}", user.getId());
            return;
        }

        invalidatePrevious(user, CodePurpose.VERIFY_EMAIL);

        String plainToken = HashUtil.randomToken(32);

        codeRepository.save(VerificationCode.builder()
                        .user(user)
                        .codeHash(HashUtil.sha256(plainToken))
                        .purpose(CodePurpose.VERIFY_EMAIL)
                        .expiresAt(Instant.now().plus(props.getVerificationTtl()))
                .build());
        String link = props.getVerificationBaseUrl() + "?token=" + plainToken;

        emailSender.send(user.getEmail(),
                emailTemplateRenderer.verificationSubject(user.getLanguage().name()),
                emailTemplateRenderer.verificationBody(user, link));
    }

    @Transactional
    public void consumeVerificationToken(String plainToken) {
        VerificationCode token = codeRepository
                .findByCodeHashAndPurpose(HashUtil.sha256(plainToken), CodePurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new InvalidCodeException("This verification link is not valid"));

        if (!token.isUsable()) {
            throw new InvalidCodeException("This link has expired. Request a new one from your profile.");
        }

        token.setConsumedAt(Instant.now());

        User user = token.getUser();
        user.setEmailVerifiedAt(Instant.now());
    }

    @Transactional
    public void sendResetCode(User user) {
        if (user.getEmail() == null) return;

        if (isRateLimited(user, CodePurpose.PASSWORD_RESET)) {
            throw new TooManyRequestsException("Too many reset requests. Try again later.");
        }

        invalidatePrevious(user, CodePurpose.PASSWORD_RESET);
        String code = randomDigits();

        codeRepository.save(VerificationCode.builder()
                        .user(user)
                        .codeHash(HashUtil.sha256(code))
                        .purpose(CodePurpose.PASSWORD_RESET)
                        .expiresAt(Instant.now().plus(props.getResetCodeTtl()))
                .build());
        emailSender.send(user.getEmail(),
                emailTemplateRenderer.resetSubject(user.getLanguage().name()),
                emailTemplateRenderer.resetBody(user, code));
    }

    @Transactional
    public void consumeResetCode(User user, String submitted) {
        VerificationCode token = codeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), CodePurpose.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidCodeException("No active code. Request a new one."));

        if (!token.isUsable()) {
            throw new InvalidCodeException("This code has expired. Request a new one.");
        }

        if (token.getAttempts() >= props.getResetMaxAttempts()) {
            token.setConsumedAt(Instant.now());
            throw new InvalidCodeException("Too many incorrect attempts. Request a new code");
        }

        token.setAttempts(token.getAttempts() + 1);

        if (!HashUtil.constantTimeEquals(HashUtil.sha256(submitted), token.getCodeHash())) {
            throw new InvalidCodeException("That code is not correct");
        }

        token.setConsumedAt(Instant.now());

        if (!user.isEmailVerified()) {
            user.setEmailVerifiedAt(Instant.now());
        }
    }

    private boolean isRateLimited(User user, CodePurpose purpose) {
        long recent = codeRepository.countRecent(user.getId(), purpose, Instant.now().minus(1, ChronoUnit.HOURS));

        if (recent >= props.getMaxSendsPerHour()) return true;

        return codeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .map(t -> Instant.now().isBefore(t.getCreatedAt().plus(props.getResendCooldown())))
                .orElse(false);
    }

    private void invalidatePrevious(User user, CodePurpose purpose) {
        codeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .ifPresent(t -> t.setConsumedAt(Instant.now()));
    }

    private String randomDigits() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeSpent() {
        int removed = codeRepository.deleteSpentBefore(Instant.now().minus(7, ChronoUnit.DAYS));
        if (removed > 0) log.info("Purged {} spent email tokens", removed);
    }
}
