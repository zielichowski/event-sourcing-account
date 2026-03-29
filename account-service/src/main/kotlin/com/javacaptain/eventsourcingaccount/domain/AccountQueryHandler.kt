package com.javacaptain.eventsourcingaccount.domain

import arrow.core.Either
import arrow.core.raise.either
import com.javacaptain.eventsourcingaccount.api.Account
import com.javacaptain.eventsourcingaccount.api.AccountQuery
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.infrastructure.AccountRepository

internal class AccountQueryHandler(
    private val accountRepository: AccountRepository,
    private val accountEventHandler: AccountEventHandler,
) {
    fun getAccount(accountQuery: AccountQuery): Either<DomainError, Account> {
        return either {
            accountRepository.getEvents(accountQuery.accountId)
                .map { accountEventHandler.state(it) }.bind()
                .map {
                    when (it) {
                        is AccountState.OpenedAccount -> Account(it.accountId.value, it.ownerId.value, it.balance.value)
                        else -> raise(DomainError.AccountNotInitializedError())
                    }
                }.bind()
        }
    }
}
