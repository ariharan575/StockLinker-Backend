package com.backend.StockLinker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service.account}")
    private String firebaseServiceAccountBase64;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            // 1. Check if already initialized (Fixes DevTools hot-reload issues)
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }

            // 2. Validate configuration existence
            if (firebaseServiceAccountBase64 == null || firebaseServiceAccountBase64.isBlank()) {
                throw new RuntimeException("Firebase credentials not found in environment variables!");
            }

            // 3. Decode the Base64 string back into a JSON InputStream in memory
            byte[] decodedBytes = Base64.getDecoder().decode(firebaseServiceAccountBase64);
            InputStream serviceAccount = new ByteArrayInputStream(decodedBytes);

            // 4. Initialize App
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            System.out.println("Firebase Initialized Successfully via Secure Environment Variable");
            return app;

        } catch (Exception e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    // Expose FirebaseAuth as a bean so we can inject it directly
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}

