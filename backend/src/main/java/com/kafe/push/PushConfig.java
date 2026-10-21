package com.kafe.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
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
import java.util.List;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    public PushSender pushSender(@Value("${app.firebase.credentials-path:}") String credentialsPath) throws IOException {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS is not set: push notifications will be logged, not sent.");
            return loggingSender();
        }
        if (!Files.isReadable(Path.of(credentialsPath))) {
            throw new IllegalStateException("Firebase credentials file not readable: " + credentialsPath);
        }

        FirebaseApp app;
        String projectId;
        try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            projectId = credentials instanceof ServiceAccountCredentials sa ? sa.getProjectId() : "unknown";
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        }
        log.info("Firebase push enabled for project: {}", projectId);
        return new FcmPushSender(FirebaseMessaging.getInstance(app));
    }

    private static PushSender loggingSender() {
        return (tokens, data) -> {
            log.info("[push disabled] would send {} to {} device(s)", data, tokens.size());
            return List.of();
        };
    }
}
