package com.codewithpcodes.glimserver.notification.email;

import com.codewithpcodes.glimserver.token.TokenRepository;
import com.codewithpcodes.glimserver.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

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

    @Async("notificationExecutor")
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.getEmail() == null || user.isEmailVerified()) return;

        if (isRate)
    }

    private boolean isRateLimited(User user, CodePurpose purpose) {
        long recent = tokenRepository.countRecent(user.getId(), )
    }
}
