package com.codewithpcodes.glimserver.payment.flutterwave;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "glim.flutterwave")
public class FlutterWaveProperties {
    private String authUrl;
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String webhookSecretHash;
    private String defaultCountryCode = "237";
    private String scenarioKey;
}
