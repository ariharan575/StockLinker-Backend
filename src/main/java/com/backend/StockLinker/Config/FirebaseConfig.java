package com.backend.StockLinker.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {

            InputStream serviceAccount;

            // First preference: Environment Variable
            String firebaseJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");

            if (firebaseJson != null && !firebaseJson.isBlank()) {

                serviceAccount = new ByteArrayInputStream(
                        firebaseJson.getBytes(StandardCharsets.UTF_8)
                );

                System.out.println("Using Firebase credentials from Environment Variable.");

            } else {

                // Fallback: Local JSON file
                serviceAccount = getClass()
                        .getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount == null) {
                    throw new RuntimeException(
                            "Firebase credentials not found. " +
                                    "Neither FIREBASE_SERVICE_ACCOUNT environment variable nor firebase-service-account.json.json exists."
                    );
                }

                System.out.println("Using Firebase credentials from firebase-service-account.json.json.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            System.out.println("Firebase Initialized Successfully");

        } catch (Exception e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}