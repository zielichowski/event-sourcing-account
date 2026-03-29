package com.javacaptain.reward.domain;

import com.javacaptain.reward.api.AccountEvent;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO encapsulation and config
@Component
public class RewardEngine {
    private final RewardRepository rewardRepository;
    private final List<RewardRule> rules;

    RewardEngine(RewardRepository rewardRepository, List<RewardRule> rules) {
        this.rewardRepository = rewardRepository;
        this.rules = rules;
    }

    public void evaluate(AccountEvent event) {
        final RewardState state = rewardRepository.getRewardState(event.accountId());
        List<RewardResult> results = rules.stream()
                .map(rule -> rule.evaluate(event, state))
                .toList();

        // 3. Sum up all earned points
        long totalEarned = results.stream()
                .mapToLong(RewardResult::earnedPoints)
                .sum();

        // 4. Update reward state
        RewardState updated = state.addPoints(totalEarned);

        // 5. Save
        rewardRepository.save(updated);
    }
}
