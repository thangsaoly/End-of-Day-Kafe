package com.kafe.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Bean
    public PushSender pushSender(FirebaseApp firebaseApp,
                                 @Value("${app.firebase.credentials-path:}") String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return loggingSender();
        }
        log.info("Firebase push enabled");
        return new FcmPushSender(FirebaseMessaging.getInstance(firebaseApp));
    }

    private static PushSender loggingSender() {
        return (tokens, data) -> {
            log.info("[push disabled] would send {} to {} device(s)", data, tokens.size());
            return List.of();
        };
    }
}
