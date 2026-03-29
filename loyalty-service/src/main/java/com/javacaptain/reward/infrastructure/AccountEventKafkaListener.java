package com.javacaptain.reward.infrastructure;

import com.account.v1.AccountEvent;
import com.account.v1.AccountOpened;
import com.account.v1.MoneyDeposited;
import com.account.v1.MoneyWithdrawn;
import com.google.protobuf.InvalidProtocolBufferException;
import com.javacaptain.reward.api.EventType;
import com.javacaptain.reward.domain.RewardEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class AccountEventKafkaListener implements EventListener {
    private final RewardEngine rewardEngine;
    private static final Logger log = LoggerFactory.getLogger(AccountEventKafkaListener.class);

    AccountEventKafkaListener(RewardEngine rewardEngine) {
        this.rewardEngine = rewardEngine;
    }

    @Override
    @KafkaListener(topics = "${spring.kafka.consumer.account-events-topic}", groupId = "accounts")
    // TODO polish mappings
    public void handle(ConsumerRecord<String, byte[]> record) {
        try {
            final AccountEvent accountEvent = AccountEvent.parseFrom(record.value());
            switch (accountEvent.getEventCase()) {
                case ACCOUNT_OPENED -> handle(accountEvent.getAccountOpened());
                case MONEY_DEPOSITED -> handle(accountEvent.getMoneyDeposited());
                case MONEY_WITHDRAWN -> handle(accountEvent.getMoneyWithdrawn());
            }

        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

    }

    private void handle(MoneyWithdrawn moneyWithdrawn) {
        final com.javacaptain.reward.api.AccountEvent accountEvent = new com.javacaptain.reward.api.AccountEvent(
                moneyWithdrawn.getAccountId(),
                EventType.WITHDRAW,
                moneyWithdrawn.getAmountWithdrawn(),
                Instant.now()
        );
        rewardEngine.evaluate(accountEvent);

    }

    private void handle(MoneyDeposited moneyDeposited) {
        final com.javacaptain.reward.api.AccountEvent accountEvent = new com.javacaptain.reward.api.AccountEvent(
                moneyDeposited.getAccountId(),
                EventType.DEPOSIT,
                moneyDeposited.getAmountDeposited(),
                Instant.now()
        );
        rewardEngine.evaluate(accountEvent);
    }

    private void handle(AccountOpened accountOpened) {
        log.info("Account opened: {}", accountOpened);
    }
}
