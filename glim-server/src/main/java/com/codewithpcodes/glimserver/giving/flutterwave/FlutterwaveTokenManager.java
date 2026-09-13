package com.codewithpcodes.glimserver.giving.flutterwave;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlutterwaveTokenManager {

    private final FlutterWaveProperties props;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    private RestClient authClient;

    public String accessToken() {
        if (isValid()) return cachedToken;

        lock.lock();
        try {
            if (isValid()) return cachedToken;
            refresh();
            return cachedToken;
        } finally {
            lock.unlock();
        }
    }

    private boolean isValid() {
        return cachedToken != null && Instant.now().isBefore(expiresAt);
    }

    private void refresh() {
        if (authClient == null) {
            authClient = RestClient.builder().baseUrl(props.getAuthUrl()).build();
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("grant_type", "client_credentials");

        JsonNode response = authClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("access_token").isMissingNode()) {
            throw new IllegalStateException("Flutterwave authentication failed.");
        }

        cachedToken = response.path("access_token").asText();

        long ttl = response.path("expires_in").asLong(600);
        expiresAt = Instant.now().plusSeconds(Math.max(ttl - 60, 30));
        log.debug("Flutterwave token refreshed, valid for {}s", ttl);
    }

    public void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }
}
