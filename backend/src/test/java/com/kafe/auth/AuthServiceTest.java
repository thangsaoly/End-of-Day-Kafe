package com.kafe.auth;

import com.kafe.dto.SessionRequest;
import com.kafe.model.Cafe;
import com.kafe.model.User;
import com.kafe.repository.CafeRepository;
import com.kafe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CafeRepository cafeRepository;

    private AuthService authService;

    private final VerifiedToken token = new VerifiedToken("uid-1", "sophea@example.com", true, "Sophea Google");

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, cafeRepository, "");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void startSession_CreatesCustomerOnFirstSignIn() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());

        User user = authService.startSession(token, new SessionRequest("Sophea", null));

        assertEquals("uid-1", user.getFirebaseUid());
        assertEquals("Sophea", user.getName());
        assertEquals("CUSTOMER", user.getRole());
        assertEquals("sophea@example.com", user.getEmail());
        assertTrue(user.isEmailConfirmed());
        assertNull(user.getCafeId());
    }

    @Test
    void startSession_FallsBackToTokenNameThenEmail() {
        when(userRepository.findByFirebaseUid(any())).thenReturn(Optional.empty());

        assertEquals("Sophea Google", authService.startSession(token, null).getName());

        VerifiedToken noName = new VerifiedToken("uid-2", "dara@example.com", false, null);
        assertEquals("dara", authService.startSession(noName, null).getName());
    }

    @Test
    void startSession_MerchantSignUpIsNotLinkedToAnyCafe() {
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());

        User user = authService.startSession(token, new SessionRequest("Shop", "merchant"));

        assertEquals("MERCHANT", user.getRole());
        assertNull(user.getCafeId());
    }

    @Test
    void startSession_ExistingUser_KeepsRoleAndSyncsEmail() {
        User existing = new User();
        existing.setFirebaseUid("uid-1");
        existing.setName("Old Name");
        existing.setRole("CUSTOMER");
        existing.setEmail("old@example.com");
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(existing));

        // A returning user can't promote themselves by sending a role
        User user = authService.startSession(token, new SessionRequest("New Name", "MERCHANT"));

        assertSame(existing, user);
        assertEquals("Old Name", user.getName());
        assertEquals("CUSTOMER", user.getRole());
        assertEquals("sophea@example.com", user.getEmail());
    }

    @Test
    void startSession_BannedUser_IsRejected() {
        User banned = new User();
        banned.setFirebaseUid("uid-1");
        banned.setBanned(true);
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(banned));

        SecurityException ex = assertThrows(SecurityException.class, () -> authService.startSession(token, null));
        assertTrue(ex.getMessage().contains("banned"));
    }

    @Test
    void startSession_DevMerchantEmail_LinksFirstCafe() {
        authService = new AuthService(userRepository, cafeRepository, "SOPHEA@example.com");
        Cafe cafe = new Cafe("Brown Coffee", 11.55, 104.92, "21:00", null);
        cafe.setId(7L);
        when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.empty());
        when(cafeRepository.findAll()).thenReturn(List.of(cafe));

        User user = authService.startSession(token, null);

        assertEquals("MERCHANT", user.getRole());
        assertEquals(7L, user.getCafeId());
    }
}
