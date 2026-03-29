package com.javacaptain.reward.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class RewardConfiguration {
    @Bean
    RewardEngine rewardEngine(RewardRepository rewardRepository) {
        return new RewardEngine(rewardRepository, List.of(new DepositPointsRule(), new FirstTransactionBonusRule()));
    }
}
