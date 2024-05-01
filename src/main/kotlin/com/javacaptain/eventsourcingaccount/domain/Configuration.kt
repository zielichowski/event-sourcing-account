package com.javacaptain.eventsourcingaccount.domain

import com.daveanthonythomas.moshipack.MoshiPack
import com.javacaptain.eventsourcingaccount.infrastructure.AccountRepository
import com.javacaptain.eventsourcingaccount.infrastructure.MessagePackSerializer
import com.javacaptain.eventsourcingaccount.infrastructure.PostgresSqlAccountRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
internal class Configuration {
    private val eventHandler = AccountEventHandler()

    @Bean
    fun accountCommandHandler(accountRepository: AccountRepository): AccountCommandHandler {
        return AccountCommandHandler(
            eventHandler,
            accountRepository
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
}
