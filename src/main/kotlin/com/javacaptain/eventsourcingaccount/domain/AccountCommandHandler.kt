package com.javacaptain.eventsourcingaccount.domain

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import com.javacaptain.eventsourcingaccount.api.AccountCommand
import com.javacaptain.eventsourcingaccount.api.DepositMoney
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OpenAccount
import com.javacaptain.eventsourcingaccount.api.WithdrawMoney
import com.javacaptain.eventsourcingaccount.infrastructure.AccountRepository

internal class AccountCommandHandler(
    private val accountEventHandler: AccountEventHandler,
    private val accountRepository: AccountRepository,
) {
    fun handle(cmd: AccountCommand): Either<DomainError, AccountEvent> {
        return either {
            return accountRepository.getEvents(cmd.accountId).flatMap {
                if (it.any { event -> event.transactionId == cmd.transactionId }) {
                    return Either.Left(
                        DomainError.DuplicatedTransactionError("Transaction with id=${cmd.transactionId.value} already exists"),
                    )
                }
                accountEventHandler.state(it).flatMap { state ->
                    when (cmd) {
                        is OpenAccount -> handleOpenAccount(state, cmd)
                        is DepositMoney -> handleDepositMoney(state, cmd)
                        is WithdrawMoney -> handleWithdrawMoney(state, cmd)
                    }
                }.flatMap { accountEvent -> accountRepository.append(accountEvent) }
            }
        }
    }

    // Number of retries should be configurable in production mode.
    fun handleWithRetries(cmd: AccountCommand, numberOfRetires: Int = 3): Either<DomainError, AccountEvent> {
        return if (numberOfRetires > 0) {
            handle(cmd).onLeft {
                if (it is DomainError.ConcurrentModificationError) {
                    handleWithRetries(cmd, numberOfRetires - 1)
                }
            }
        } else {
            // Internal error, we are unable to process request
            throw IllegalStateException("Unable to process $cmd request")
        }
    }

    private fun handleOpenAccount(
        state: AccountState,
        cmd: OpenAccount,
    ) = either {
        when (state) {
            is AccountState.OpenedAccount ->
                raise(
                    DomainError.AccountAlreadyOpenedError("Account with id=${state.accountId} has been already opened"),
                )

            AccountState.NotInitialized ->
                AccountOpened(
                    cmd.accountId,
                    1,
                    cmd.transactionId,
                    cmd.ownerId,
                    Money(0).bind(),
                )
        }
    }

    private fun handleDepositMoney(
        state: AccountState,
        cmd: DepositMoney,
    ) = either {
        when (state) {
            is AccountState.OpenedAccount ->
                MoneyDeposited(
                    cmd.accountId,
                    state.version.inc(),
                    cmd.transactionId,
                    cmd.deposit,
                    state.balance,
                )

            AccountState.NotInitialized ->
                raise(
                    DomainError.AccountNotInitializedError("Unable to deposit money, account not initialized"),
                )
        }
    }

    private fun handleWithdrawMoney(
        state: AccountState,
        cmd: WithdrawMoney,
    ) = either {
        when (state) {
            is AccountState.OpenedAccount ->
                if (state.balance.subtract(cmd.withdraw).value > 0) {
                    MoneyWithdrawn(
                        cmd.accountId,
                        state.version.inc(),
                        cmd.transactionId,
                        cmd.withdraw,
                        state.balance,
                    )
                } else {
                    raise(
                        DomainError.InsufficientBalanceError(
                            "Unable to withdraw money=${cmd.withdraw}, current balance=${state.balance}",
                        ),
                    )
                }

            AccountState.NotInitialized ->
                raise(
                    DomainError.AccountNotInitializedError("Unable to withdraw money, account not initialized"),
                )
        }
    }
}
