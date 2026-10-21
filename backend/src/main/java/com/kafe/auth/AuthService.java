package com.kafe.auth;

import com.kafe.dto.SessionRequest;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final CafeRepository cafeRepository;
    private final String devMerchantEmail;

    public AuthService(UserRepository userRepository,
                       CafeRepository cafeRepository,
                       @Value("${app.dev-merchant-email:}") String devMerchantEmail) {
        this.userRepository = userRepository;
        this.cafeRepository = cafeRepository;
        this.devMerchantEmail = devMerchantEmail;
    }

    /**
     * Called after every Firebase sign-in. Creates the backend account on first use and
     * keeps the email in sync with Firebase.
     */
    @Transactional
    public User startSession(VerifiedToken token, SessionRequest request) {
        User user = userRepository.findByFirebaseUid(token.uid())
                .orElseGet(() -> newUser(token, request));

        user.setEmail(token.email());
        user.setEmailConfirmed(token.emailVerified());

        if (user.isBanned()) {
            throw new SecurityException("Your account has been banned due to multiple no-shows!");
        }
        return userRepository.save(user);
    }

    private User newUser(VerifiedToken token, SessionRequest request) {
        String requestedName = request != null ? request.getName() : null;
        String requestedRole = request != null ? request.getRole() : null;

        User user = new User();
        user.setFirebaseUid(token.uid());
        user.setName(firstNonBlank(requestedName, token.name(), emailLocalPart(token.email()), "Kafe User"));
        user.setRole("MERCHANT".equalsIgnoreCase(requestedRole) ? "MERCHANT" : "CUSTOMER");

        // Local development: make one email the demo merchant for the first cafe
        if (!devMerchantEmail.isBlank() && devMerchantEmail.equalsIgnoreCase(token.email())) {
            cafeRepository.findAll().stream().findFirst().ifPresent(cafe -> {
                user.setRole("MERCHANT");
                user.setCafeId(cafe.getId());
                log.info("Dev merchant {} linked to cafe: {}", token.email(), cafe.getName());
            });
        }
        return user;
    }

    private static String emailLocalPart(String email) {
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
