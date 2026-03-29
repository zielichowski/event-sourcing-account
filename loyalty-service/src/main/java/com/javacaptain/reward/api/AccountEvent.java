package com.javacaptain.reward.api;

import java.time.Instant;

public record AccountEvent(
        String accountId,
        EventType eventType,
        long amount,
        Instant timestampUTC
) {
}

