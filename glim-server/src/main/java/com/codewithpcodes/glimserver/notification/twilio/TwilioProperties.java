package com.codewithpcodes.glimserver.notification.twilio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "glim.twilio")
public class TwilioProperties {
    private String accountSid;
    private String authToken;
    private String messagingServiceSid;
    private String fromNumber;
    private String statusCallbackUrl;
}
