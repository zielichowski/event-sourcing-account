package com.javacaptain.eventsourcingaccount.infrastructure

import com.javacaptain.eventsourcingaccount.domain.AccountEvent

internal interface EventPublisher {
    fun publish(accountEvent: AccountEvent)
}