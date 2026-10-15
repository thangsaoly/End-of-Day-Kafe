package com.kafe.service;

import com.kafe.model.DeviceToken;
import com.kafe.model.User;
import com.kafe.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.clock = clock;
    }

    // A token identifies one app install. If someone else logs in on the same phone,
    // the token moves to them so the previous user stops getting their alerts.
    @Transactional
    public void register(String token, User user) {
        String clean = requireToken(token);
        DeviceToken deviceToken = deviceTokenRepository.findByToken(clean)
                .orElseGet(() -> new DeviceToken(clean, user));
        deviceToken.setUser(user);
        deviceToken.setUpdatedAt(LocalDateTime.now(clock));
        deviceTokenRepository.save(deviceToken);
    }

    // Called on logout. Only the token's current owner can remove it.
    @Transactional
    public void unregister(String token, User user) {
        deviceTokenRepository.findByToken(requireToken(token))
                .filter(deviceToken -> deviceToken.getUser().getId().equals(user.getId()))
                .ifPresent(deviceTokenRepository::delete);
    }

    private static String requireToken(String token) {
        if (token == null || token.isBlank() || token.length() > 512) {
            throw new IllegalArgumentException("Invalid device token");
        }
        return token.trim();
    }
}
