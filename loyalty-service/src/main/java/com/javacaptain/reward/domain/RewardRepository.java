package com.javacaptain.reward.domain;

public interface RewardRepository {
    RewardState getRewardState(String accountId);

    void save(RewardState updated);
}
