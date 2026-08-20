package com.codewithpcodes.glimserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${glim.firebase-service-account-b64}")
    private String serviceAccountBase64;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        var credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountBase64))
        );

        FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(credentials).build());

        log.info("Firebase initialised");
    }
}
