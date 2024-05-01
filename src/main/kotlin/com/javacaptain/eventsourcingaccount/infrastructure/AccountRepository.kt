package com.javacaptain.eventsourcingaccount.infrastructure

import arrow.core.Either
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.domain.AccountEvent

internal interface AccountRepository {
    fun getEvents(accountId: AccountId): Either<DomainError, Sequence<AccountEvent>>

    fun append(accountEvent: AccountEvent): Either<DomainError, AccountEvent>
}
