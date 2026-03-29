package com.javacaptain.reward.api;

import java.time.Instant;

public record RewardEvent(
        String accountId,
        int pointsEarned,
        String reason,
        Instant timestampUTC
) {
}
