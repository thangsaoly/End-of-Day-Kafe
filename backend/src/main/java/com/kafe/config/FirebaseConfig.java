package com.kafe.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.kafe.auth.FirebaseAdminTokenVerifier;
import com.kafe.auth.FirebaseTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp(@Value("${app.firebase.credentials-path:}") String credentialsPath,
                                   @Value("${app.firebase.project-id}") String projectId) throws IOException {
        // Tests start several Spring contexts in one JVM; reuse the app if it exists
        for (FirebaseApp existing : FirebaseApp.getApps()) {
            if (FirebaseApp.DEFAULT_APP_NAME.equals(existing.getName())) {
                return existing;
            }
        }

        GoogleCredentials credentials;
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            if (!Files.isReadable(Path.of(credentialsPath))) {
                throw new IllegalStateException("Firebase credentials file not readable: " + credentialsPath);
            }
            try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(serviceAccount);
            }
            log.info("Firebase initialised with service account for project: {}", projectId);
        } else {
            // Verifying ID tokens only needs Google's public keys and the project ID, so login
            // works without a key. Sending pushes does need one (see PushConfig).
            credentials = GoogleCredentials.create(new AccessToken("no-service-account", null));
            log.warn("FIREBASE_CREDENTIALS is not set: logins work, push notifications are disabled.");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseTokenVerifier firebaseTokenVerifier(FirebaseApp firebaseApp) {
        return new FirebaseAdminTokenVerifier(FirebaseAuth.getInstance(firebaseApp));
    }
}
