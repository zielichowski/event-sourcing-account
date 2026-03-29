package com.javacaptain.reward.domain;

import com.javacaptain.reward.api.AccountEvent;
import com.javacaptain.reward.api.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

sealed interface RewardRule {
    RewardResult evaluate(AccountEvent accountEvent, RewardState rewardState);
}

final class DepositPointsRule implements RewardRule {
    private static final Logger log = LoggerFactory.getLogger(DepositPointsRule.class);

    @Override
    public RewardResult evaluate(AccountEvent event, RewardState state) {
        if (event.eventType() == EventType.DEPOSIT) {

            long points = event.amount() / 100;
            if (state.totalPoints() < 1000 && state.totalPoints() + points >= 1000) {
                points += 50; // milestone bonus
                log.info("Deposit Bonus + Milestone Bonus: {}", points);
                return new RewardResult(points);
            }
            log.info("Deposit Bonus {}", points);
            return new RewardResult(points);


        }
        log.info("No Deposit bonus points");
        return new RewardResult(0);
    }
}

final class FirstTransactionBonusRule implements RewardRule {
    private static final Logger log = LoggerFactory.getLogger(FirstTransactionBonusRule.class);
    @Override
    public RewardResult evaluate(AccountEvent accountEvent, RewardState rewardState) {
        if (accountEvent.eventType() != EventType.DEPOSIT) {
            return new RewardResult(0);
        }
        if (rewardState.transactionCount() == 0) {
            log.info("First transaction bonus 100 points");
            return new RewardResult(100);
        }
        return new RewardResult(0);
    }
}