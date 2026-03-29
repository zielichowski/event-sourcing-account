package com.javacaptain.eventsourcingaccount.domain

import com.daveanthonythomas.moshipack.MoshiPack
import com.javacaptain.eventsourcingaccount.infrastructure.AccountRepository
import com.javacaptain.eventsourcingaccount.infrastructure.EventPublisher
import com.javacaptain.eventsourcingaccount.infrastructure.KafkaEventPublisher
import com.javacaptain.eventsourcingaccount.infrastructure.MessagePackSerializer
import com.javacaptain.eventsourcingaccount.infrastructure.PostgresSqlAccountRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import javax.sql.DataSource

@Configuration
internal class Configuration {
    private val eventHandler = AccountEventHandler()

    @Bean
    fun accountCommandHandler(
        accountRepository: AccountRepository,
        eventPublisher: EventPublisher,
    ): AccountCommandHandler {
        return AccountCommandHandler(
            eventHandler,
            accountRepository,
            eventPublisher
        )
    }

    @Bean
    fun accountRepository(dataSource: DataSource): AccountRepository {
        return PostgresSqlAccountRepository(dataSource, MessagePackSerializer(MoshiPack()))
    }

    @Bean
    fun accountQueryHandler(accountRepository: AccountRepository): AccountQueryHandler {
        return AccountQueryHandler(accountRepository, eventHandler)
    }

    @Bean
    fun eventPublisher(
        kafkaTemplate: KafkaTemplate<String, ByteArray>,
        @Value("\${spring.kafka.producer.account-events-topic}") accountEventsTopic: String,
    ): EventPublisher {
        return KafkaEventPublisher(kafkaTemplate, accountEventsTopic)
    }
}
