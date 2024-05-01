package com.javacaptain.eventsourcingaccount.domain

import arrow.core.Either
import arrow.core.raise.either
import com.javacaptain.eventsourcingaccount.api.DomainError

internal class AccountEventHandler {
    fun state(events: Sequence<AccountEvent>): Either<DomainError, AccountState> {
        val notInitialized: AccountState = AccountState.NotInitialized

        return either {
            events.fold(notInitialized) { currentState, event -> evolve(currentState, event).bind() }
        }
    }

    private fun evolve(
        currentState: AccountState,
        event: AccountEvent,
    ): Either<DomainError, AccountState> =
        either {
            when (currentState) {
                is AccountState.NotInitialized ->
                    when (event) {
                        is AccountOpened ->
                            AccountState.OpenedAccount(
                                event.accountId,
                                event.ownerId,
                                event.initialBalance,
                                1,
                            )
                        else ->
                            raise(
                                DomainError.AccountNotInitializedError(
                                    "Unable to apply event=$event. Account has not been initialized",
                                ),
                            )
                    }
                is AccountState.OpenedAccount ->
                    when (event) {
                        is AccountOpened ->
                            raise(
                                DomainError.AccountAlreadyOpenedError(
                                    "Unable to apply event=$event. Account with id=${event.accountId} has been already opened",
                                ),
                            )
                        is MoneyDeposited ->
                            currentState.copy(
                                balance = currentState.balance.add(event.amountDeposited),
                                version = event.version,
                            )
                        is MoneyWithdrawn ->
                            currentState.copy(
                                balance = currentState.balance.subtract(event.amountWithdrawn),
                                version = event.version,
                            )
                    }
            }
        }
}
