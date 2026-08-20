package com.codewithpcodes.glimserver.config;

import com.codewithpcodes.glimserver.notification.twilio.TwilioProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(TwilioProperties.class)
@RequiredArgsConstructor
public class TwilioConfig {

    private final TwilioProperties props;

    @PostConstruct
    public void initTwilio() {
        if (props.getAccountSid() == null || props.getAuthToken() == null) {
            throw new IllegalStateException("Twilio credentials are not configured.");
        }
        Twilio.init(props.getAccountSid(), props.getAuthToken());
        log.info("Twilio initialised for account {}…", props.getAccountSid().substring(0, 8));
    }
}
