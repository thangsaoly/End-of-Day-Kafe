package com.kafe.auth;

import com.kafe.model.User;
import com.kafe.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Real security filter chain and controllers; only Firebase's token check is replaced
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FirebaseAuthFlowIntegrationTest {

    @MockBean
    private FirebaseTokenVerifier tokenVerifier;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Only "good-token" is valid; anything else is treated as forged
        when(tokenVerifier.verify(anyString())).thenAnswer(call -> {
            if ("good-token".equals(call.getArgument(0))) {
                return new VerifiedToken("flow-uid", "flow@example.com", true, "Flow User");
            }
            throw new InvalidTokenException("bad", null);
        });
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByFirebaseUid("flow-uid").ifPresent(userRepository::delete);
    }

    @Test
    void protectedEndpoint_WithoutToken_IsRefused() throws Exception {
        mockMvc.perform(get("/api/reservations")).andExpect(status().isForbidden());
    }

    @Test
    void removedPasswordLogin_Returns404() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void session_WithInvalidToken_Returns401() throws Exception {
        mockMvc.perform(post("/api/auth/session").header("Authorization", "Bearer forged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenButNoAccount_CannotUseApi_UntilSessionCreatesIt() throws Exception {
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/session")
                        .header("Authorization", "Bearer good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sophea\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sophea"))
                .andExpect(jsonPath("$.email").value("flow@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk());
    }

    @Test
    void session_WithoutBody_UsesTokenName() throws Exception {
        mockMvc.perform(post("/api/auth/session").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Flow User"));
    }

    @Test
    void bannedUser_IsRefusedEverywhere() throws Exception {
        User banned = new User();
        banned.setFirebaseUid("flow-uid");
        banned.setName("Banned");
        banned.setBanned(true);
        userRepository.save(banned);

        mockMvc.perform(post("/api/auth/session").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer good-token"))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.findByFirebaseUid("flow-uid").orElseThrow().isBanned());
    }
}
