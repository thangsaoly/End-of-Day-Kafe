package com.kafe.push;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    // FCM accepts at most 500 tokens per multicast request
    static final int MAX_BATCH = 500;

    private final FirebaseMessaging messaging;

    public FcmPushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public List<String> send(List<String> tokens, Map<String, String> data) {
        List<String> invalidTokens = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += MAX_BATCH) {
            List<String> batch = tokens.subList(start, Math.min(start + MAX_BATCH, tokens.size()));
            // Data-only + high priority so the app builds the notification itself,
            // in the foreground and the background alike
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            try {
                BatchResponse response = messaging.sendEachForMulticast(message);
                List<SendResponse> results = response.getResponses();
                for (int i = 0; i < results.size(); i++) {
                    SendResponse result = results.get(i);
                    if (result.isSuccessful()) {
                        continue;
                    }
                    FirebaseMessagingException error = result.getException();
                    log.warn("Push to one device failed: {} ({})",
                            error.getMessagingErrorCode(), error.getMessage());
                    if (isInvalidToken(error)) {
                        invalidTokens.add(batch.get(i));
                    }
                }
                log.info("Push sent: {} ok, {} failed", response.getSuccessCount(), response.getFailureCount());
            } catch (FirebaseMessagingException e) {
                log.error("Push batch failed: {}", e.getMessage());
            }
        }
        return invalidTokens;
    }

    private static boolean isInvalidToken(FirebaseMessagingException e) {
        if (e == null) {
            return false;
        }
        MessagingErrorCode code = e.getMessagingErrorCode();
        // Not INVALID_ARGUMENT: it can also mean a bad payload, and deleting on it
        // could wipe every token
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.SENDER_ID_MISMATCH;
    }
}
