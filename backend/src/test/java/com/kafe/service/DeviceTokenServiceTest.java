package com.kafe.service;

import com.kafe.model.DeviceToken;
import com.kafe.model.User;
import com.kafe.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private DeviceTokenService service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T05:00:00Z"), ZoneId.of("Asia/Phnom_Penh"));
        service = new DeviceTokenService(deviceTokenRepository, clock);
        alice = new User("Alice", "011111111", "hash");
        alice.setId(1L);
        bob = new User("Bob", "022222222", "hash");
        bob.setId(2L);
    }

    @Test
    void register_SavesNewToken() {
        when(deviceTokenRepository.findByToken("tok-1")).thenReturn(Optional.empty());

        service.register(" tok-1 ", alice);

        ArgumentCaptor<DeviceToken> saved = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(saved.capture());
        assertEquals("tok-1", saved.getValue().getToken());
        assertEquals(alice, saved.getValue().getUser());
    }

    @Test
    void register_MovesExistingTokenToNewUser() {
        DeviceToken existing = new DeviceToken("tok-1", alice);
        when(deviceTokenRepository.findByToken("tok-1")).thenReturn(Optional.of(existing));

        service.register("tok-1", bob);

        assertEquals(bob, existing.getUser());
        verify(deviceTokenRepository).save(existing);
    }

    @Test
    void register_RejectsBlankToken() {
        assertThrows(IllegalArgumentException.class, () -> service.register("  ", alice));
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void unregister_DeletesOwnToken() {
        DeviceToken existing = new DeviceToken("tok-1", alice);
        when(deviceTokenRepository.findByToken("tok-1")).thenReturn(Optional.of(existing));

        service.unregister("tok-1", alice);

        verify(deviceTokenRepository).delete(existing);
    }

    @Test
    void unregister_IgnoresSomeoneElsesToken() {
        when(deviceTokenRepository.findByToken("tok-1")).thenReturn(Optional.of(new DeviceToken("tok-1", alice)));

        service.unregister("tok-1", bob);

        verify(deviceTokenRepository, never()).delete(any());
    }
}
