package com.kafe.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserJsonTest {

    @Test
    void serializedUser_DoesNotExposeSecrets() throws Exception {
        User user = new User("Sophea", "012345678", "$2a$10$secretHash", "CUSTOMER");
        user.setEmailVerificationCode("777111");

        String json = new ObjectMapper().writeValueAsString(user);

        assertFalse(json.contains("passwordHash"), json);
        assertFalse(json.contains("secretHash"), json);
        assertFalse(json.contains("emailVerificationCode"), json);
        assertFalse(json.contains("777111"), json);
        assertTrue(json.contains("Sophea"));
    }
}
