package com.kafe.push;

import java.util.List;
import java.util.Map;

public interface PushSender {

    /**
     * Sends a data message to each token.
     *
     * @return tokens FCM reported as no longer valid (app uninstalled, token rotated);
     *         callers should delete them
     */
    List<String> send(List<String> tokens, Map<String, String> data);
}
