package com.javacaptain.reward.web;

import com.javacaptain.reward.domain.RewardRepository;
import com.javacaptain.reward.domain.RewardState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RewardController {
    private final RewardRepository rewardRepository;

    RewardController(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @GetMapping("/rewards/{accountId}")
    public RewardState get(@PathVariable String accountId) {
        return rewardRepository.getRewardState(accountId);
    }
}
