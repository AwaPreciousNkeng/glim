package com.codewithpcodes.glimserver.giving;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "glim.giving")
public class GivingProperties {
    private long minimumAmount = 500;
    private int pendingTimeoutMinutes = 5;
    private int reviewTimeoutMinutes = 60;
    private int maxPollAttempts = 12;
    private String redirectUrl;
}
