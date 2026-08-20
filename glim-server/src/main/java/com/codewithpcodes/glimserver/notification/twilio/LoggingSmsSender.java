package com.codewithpcodes.glimserver.notification.twilio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class LoggingSmsSender implements SmsSender {
    @Override
    public void send(String toE164, String message) {
        log.info("═══ SMS → {} ═══\n{}\n═══════════════", toE164, message);
    }

    @Override
    public SmsOutcome sendAndReport(String toE164, String message) {
        send(toE164, message);
        return new SmsOutcome(true, "LOCAL-" + UUID.randomUUID(), null);
    }
}
