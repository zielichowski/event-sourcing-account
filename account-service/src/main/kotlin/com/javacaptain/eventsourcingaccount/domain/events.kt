package com.javacaptain.eventsourcingaccount.domain

import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId

sealed interface AccountEvent {
    val accountId: AccountId
    val transactionId: TransactionId
    val version: Long
}

data class AccountOpened(
    override val accountId: AccountId,
    override val version: Long,
    override val transactionId: TransactionId,
    val ownerId: OwnerId,
    val initialBalance: Money,
) : AccountEvent

data class MoneyDeposited(
    override val accountId: AccountId,
    override val version: Long,
    override val transactionId: TransactionId,
    val amountDeposited: Money,
    val balance: Money,
) : AccountEvent

data class MoneyWithdrawn(
    override val accountId: AccountId,
    override val version: Long,
    override val transactionId: TransactionId,
    val amountWithdrawn: Money,
    val balance: Money,
) : AccountEvent
