package com.javacaptain.reward.domain;

import java.time.Instant;

public record RewardState(
        String accountId,
        long totalPoints,
        long lastEarnedPoints,
        int transactionCount,
        Instant lastUpdated
) {
    public RewardState addPoints(long points) {
        return new RewardState(
                accountId,
                totalPoints + points,
                points,
                transactionCount + 1,
                Instant.now()
        );
    }
}
