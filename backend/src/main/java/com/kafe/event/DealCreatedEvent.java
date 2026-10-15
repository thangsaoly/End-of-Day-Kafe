package com.kafe.event;

// Published inside the createDeal transaction; listeners act only after it commits
public record DealCreatedEvent(Long dealId) {
}
