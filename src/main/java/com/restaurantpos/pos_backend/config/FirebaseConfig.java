package com.restaurantpos.pos_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// Initializes the Firebase Admin SDK once at application startup. Reads the
// service account credentials from an environment variable (FIREBASE_SERVICE_ACCOUNT_JSON)
// when deployed to Railway — since the actual JSON key file is gitignored and
// never committed — falling back to the local file on disk during local
// development, where the file is simply placed on the classpath directly.
@Component
@Slf4j
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String firebaseServiceAccountJson;

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;

            if (firebaseServiceAccountJson != null && !firebaseServiceAccountJson.isBlank()) {
                serviceAccount = new ByteArrayInputStream(
                        firebaseServiceAccountJson.getBytes(StandardCharsets.UTF_8));
            } else {
                serviceAccount = getClass()
                        .getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");
            }

            if (serviceAccount == null) {
                log.warn("Firebase service account credentials not found (neither env var nor local file) — push notifications will be disabled.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }
}