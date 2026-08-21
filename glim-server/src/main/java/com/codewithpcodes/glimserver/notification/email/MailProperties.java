package com.codewithpcodes.glimserver.notification.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "glim.mail")
public class MailProperties {
    private String from = "awaprecious3t@gmail.com";
    private String fromName = "GLIM City";
    private Duration verificationTtl = Duration.ofHours(24);
    private Duration resetCodeTtl = Duration.ofMinutes(15);
    private int resetMaxAttempts = 5;
    private Duration resendCooldown = Duration.ofSeconds(60);
    private int maxSendsPerHour = 5;
}
