package com.kafe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserJsonTest {

    @Test
    void serializedUser_DoesNotExposeSecrets() throws Exception {
        User user = new User("Sophea", "012345678", "$2a$10$secretHash", "CUSTOMER");
        user.setFirebaseUid("firebase-uid-123");

        String json = new ObjectMapper().writeValueAsString(user);

        assertFalse(json.contains("passwordHash"), json);
        assertFalse(json.contains("secretHash"), json);
        assertFalse(json.contains("firebaseUid"), json);
        assertFalse(json.contains("firebase-uid-123"), json);
        assertTrue(json.contains("Sophea"));
    }
}
