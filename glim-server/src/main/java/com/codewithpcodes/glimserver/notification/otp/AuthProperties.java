package com.codewithpcodes.glimserver.notification.otp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "glim.otp")
public class AuthProperties {
    private int otpLength = 6;
    private Duration otpTtl = Duration.ofMinutes(10);
    private int otpMaxAttempts = 5;
    private Duration otpResendCooldown = Duration.ofSeconds(30);
    private int otpMaxResendsPerHour = 3;
}
