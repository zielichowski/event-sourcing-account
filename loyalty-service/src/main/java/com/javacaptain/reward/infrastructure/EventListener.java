package com.javacaptain.reward.infrastructure;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface EventListener {
    void handle(ConsumerRecord<String, byte[]> record);
}
