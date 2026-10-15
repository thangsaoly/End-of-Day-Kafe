package com.kafe.push;

import com.kafe.dto.CreateDealRequest;
import com.kafe.model.Cafe;
import com.kafe.model.DeviceToken;
import com.kafe.model.Favorite;
import com.kafe.model.SurplusDeal;
import com.kafe.model.User;
import com.kafe.repository.*;
import com.kafe.service.KafeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

// Full Spring context: checks the after-commit + async wiring, not just the notifier logic
@SpringBootTest
@ActiveProfiles("test")
public class NewDealNotifierIntegrationTest {

    @MockBean
    private PushSender pushSender;

    @Autowired private KafeService kafeService;
    @Autowired private CafeRepository cafeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private DeviceTokenRepository deviceTokenRepository;
    @Autowired private SurplusDealRepository surplusDealRepository;

    @AfterEach
    void cleanUp() {
        deviceTokenRepository.deleteAll();
        favoriteRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createDeal_NotifiesOnlyFavouritersDevices_AndRemovesInvalidTokens() {
        Cafe cafe = cafeRepository.save(new Cafe("Notify Test Cafe", 11.55, 104.92, "21:00", null));
        Cafe otherCafe = cafeRepository.save(new Cafe("Other Cafe", 11.56, 104.93, "21:00", null));

        User fan = userRepository.save(new User("Fan", "notify_fan", "hash"));
        User otherFan = userRepository.save(new User("Other Fan", "notify_other", "hash"));
        User merchant = new User("Merchant", "notify_merchant", "hash", "MERCHANT");
        merchant.setCafeId(cafe.getId());
        merchant = userRepository.save(merchant);

        favoriteRepository.save(new Favorite(fan, cafe));
        favoriteRepository.save(new Favorite(otherFan, otherCafe));
        deviceTokenRepository.save(new DeviceToken("fan-phone", fan));
        deviceTokenRepository.save(new DeviceToken("fan-old-phone", fan));
        deviceTokenRepository.save(new DeviceToken("other-phone", otherFan));

        when(pushSender.send(anyList(), anyMap())).thenReturn(List.of("fan-old-phone"));

        CreateDealRequest request = new CreateDealRequest();
        request.setTitle("Evening Bread Bag");
        request.setOriginalPrice(6.0);
        request.setDiscountPrice(2.0);
        request.setQuantity(2);
        request.setPickupStart("19:00");
        request.setPickupEnd("20:30");
        SurplusDeal deal = kafeService.createDeal(request, merchant);

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, String>> data = ArgumentCaptor.forClass(Map.class);
        verify(pushSender, timeout(5000)).send(tokens.capture(), data.capture());

        assertEquals(List.of("fan-old-phone", "fan-phone"), tokens.getValue().stream().sorted().toList());
        assertEquals("NEW_DEAL", data.getValue().get("type"));
        assertEquals(String.valueOf(deal.getId()), data.getValue().get("dealId"));
        assertEquals("New deal at Notify Test Cafe", data.getValue().get("title"));

        // Invalid token cleanup happens right after send on the async thread
        long deadline = System.currentTimeMillis() + 5000;
        while (deviceTokenRepository.findByToken("fan-old-phone").isPresent() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(deviceTokenRepository.findByToken("fan-old-phone").isEmpty());
        assertTrue(deviceTokenRepository.findByToken("fan-phone").isPresent());

        surplusDealRepository.deleteById(deal.getId());
    }
}
