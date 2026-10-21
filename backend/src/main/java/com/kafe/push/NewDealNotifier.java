package com.kafe.push;

import com.kafe.event.DealCreatedEvent;
import com.kafe.model.DeviceToken;
import com.kafe.model.SurplusDeal;
import com.kafe.repository.DeviceTokenRepository;
import com.kafe.repository.FavoriteRepository;
import com.kafe.repository.SurplusDealRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

// Tells users who favourited a cafe that it just posted a new deal
@Component
public class NewDealNotifier {

    private static final Logger log = LoggerFactory.getLogger(NewDealNotifier.class);

    private final SurplusDealRepository surplusDealRepository;
    private final FavoriteRepository favoriteRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;

    public NewDealNotifier(SurplusDealRepository surplusDealRepository,
                           FavoriteRepository favoriteRepository,
                           DeviceTokenRepository deviceTokenRepository,
                           PushSender pushSender) {
        this.surplusDealRepository = surplusDealRepository;
        this.favoriteRepository = favoriteRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushSender = pushSender;
    }

    // Runs only after the deal is committed, on a separate thread, so a slow or
    // failing push can never delay or roll back the merchant's request
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDealCreated(DealCreatedEvent event) {
        SurplusDeal deal = surplusDealRepository.findById(event.dealId()).orElse(null);
        if (deal == null) {
            return;
        }

        List<Long> userIds = favoriteRepository.findUserIdsByCafeId(deal.getCafe().getId());
        if (userIds.isEmpty()) {
            return;
        }
        List<String> tokens = deviceTokenRepository.findByUserIdIn(userIds).stream()
                .map(DeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        List<String> invalid = pushSender.send(tokens, buildData(deal));
        if (!invalid.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(invalid);
            log.info("Removed {} invalid device token(s)", invalid.size());
        }
    }

    static Map<String, String> buildData(SurplusDeal deal) {
        return Map.of(
                "type", "NEW_DEAL",
                "dealId", String.valueOf(deal.getId()),
                "cafeId", String.valueOf(deal.getCafe().getId()),
                "title", "New deal at " + deal.getCafe().getName(),
                "body", String.format("%s: $%.2f (was $%.2f). Pickup %s-%s",
                        deal.getTitle(), deal.getDiscountPrice(), deal.getOriginalPrice(),
                        deal.getPickupStart(), deal.getPickupEnd())
        );
    }
}
