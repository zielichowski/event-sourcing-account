package com.javacaptain.reward.infrastructure;

import com.javacaptain.reward.domain.RewardRepository;
import com.javacaptain.reward.domain.RewardState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
class InMemoryRewardRepository implements RewardRepository {
    private final ConcurrentMap<String, RewardState> store = new ConcurrentHashMap<>();

    @Override
    public RewardState getRewardState(String accountId) {
        return store.computeIfAbsent(accountId, id -> new RewardState(
                id,
                0,
                0,
                0,
                Instant.now()
        ));
    }

    @Override
    public void save(RewardState updated) {
        store.put(updated.accountId(), updated);
    }
}