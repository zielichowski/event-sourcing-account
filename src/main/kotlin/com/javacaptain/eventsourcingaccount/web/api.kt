package com.javacaptain.eventsourcingaccount.web

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DepositMoney
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OpenAccount
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId
import com.javacaptain.eventsourcingaccount.api.WithdrawMoney
import java.util.*

data class ApiErrorResponse(
    val errors: List<String>,
)

data class OpenAccountRequest(
    val transactionId: String,
    val ownerId: String,
) {
    fun validate(): Either<NonEmptyList<DomainError>, OpenAccount> {
        return either {
            zipOrAccumulate(
                { ensure(transactionId.isNotBlank() && transactionId.isNotEmpty()) { DomainError.EmptyTransactionId() } },
                { ensure(ownerId.isNotBlank() && ownerId.isNotEmpty()) { DomainError.EmptyOwnerId() } },
                { ensure(transactionId.matches(UUID_REGEX)) { DomainError.InvalidTransactionIdFormat() } },
                { ensure(ownerId.matches(UUID_REGEX)) { DomainError.InvalidOwnerIdFormat() } },
            ) { _, _, _, _ ->
                OpenAccount(
                    AccountId(UUID.randomUUID()),
                    OwnerId(UUID.fromString(ownerId)),
                    TransactionId(UUID.fromString(transactionId)),
                )
            }
        }
    }
}

data class DepositMoneyRequest(
    val money: Long,
    val transactionId: String,
) {
    fun validate(accountId: String): Either<NonEmptyList<DomainError>, DepositMoney> {
        return either {
            zipOrAccumulate(
                { ensure(transactionId.isNotBlank() && transactionId.isNotEmpty()) { DomainError.EmptyTransactionId() } },
                { ensure(transactionId.matches(UUID_REGEX)) { DomainError.InvalidTransactionIdFormat() } },
                { ensure(accountId.matches(UUID_REGEX)) { DomainError.InvalidAccountIdFormat() } },
                { Money(money).bind() },
            ) { _, _, _, money ->
                DepositMoney(
                    AccountId(UUID.fromString(accountId)),
                    money,
                    TransactionId(UUID.fromString(transactionId)),
                )
            }
        }
    }
}

data class WithdrawMoneyRequest(
    val money: Long,
    val transactionId: String,
) {
    fun validate(accountId: String): Either<NonEmptyList<DomainError>, WithdrawMoney> {
        return either {
            zipOrAccumulate(
                { ensure(transactionId.isNotBlank() && transactionId.isNotEmpty()) { DomainError.EmptyTransactionId() } },
                { ensure(transactionId.matches(UUID_REGEX)) { DomainError.InvalidTransactionIdFormat() } },
                { ensure(accountId.matches(UUID_REGEX)) { DomainError.InvalidAccountIdFormat() } },
                { Money(money).bind() },
            ) { _, _, _, money ->
                WithdrawMoney(
                    AccountId(UUID.fromString(accountId)),
                    money,
                    TransactionId(UUID.fromString(transactionId)),
                )
            }
        }
    }
}

private val UUID_REGEX =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$".toRegex()
