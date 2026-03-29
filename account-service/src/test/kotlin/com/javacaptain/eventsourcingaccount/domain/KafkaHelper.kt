package com.javacaptain.eventsourcingaccount.domain

import com.account.v1.AccountEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

// Test consumer to test kafka producer including helper methods
@Component
class KafkaHelper {

    private val events: Queue<AccountEvent> = ConcurrentLinkedQueue()
    private val logger = LoggerFactory.getLogger(KafkaHelper::class.java)

    @KafkaListener(
        topics = ["\${spring.kafka.producer.account-events-topic}"],
        groupId = "integrationtest"
    )
    fun onMessage(record: ConsumerRecord<String, ByteArray>) {
        val event = AccountEvent.parseFrom(record.value())

        logger.info(
            "Received account event: key={}, type={}",
            record.key(),
            event.eventCase
        )

        events.add(event)
    }

    fun awaitNextEvent(): AccountEvent {
        Awaitility.await()
            .atMost(Durations.ONE_SECOND)
            .pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
            .until { events.peek() != null }
        return events.poll()!! // safely consume the event
    }

    fun assertNoEvent(
        duration: Duration = Duration.ofMillis(500),
        pollInterval: Duration = Duration.ofMillis(50),
    ) {
        Awaitility.await()
            .atMost(duration)
            .pollInterval(pollInterval)
            .until {
                events.isEmpty()
            }
    }

    fun clearQueuedEvents() {
        events.clear()
    }
}
