package com.javacaptain.eventsourcingaccount.api

sealed interface AccountCommand {
    val accountId: AccountId
    val transactionId: TransactionId
}

class OpenAccount(override val accountId: AccountId, val ownerId: OwnerId, override val transactionId: TransactionId) :
    AccountCommand

class DepositMoney(override val accountId: AccountId, val deposit: Money, override val transactionId: TransactionId) :
    AccountCommand

class WithdrawMoney(override val accountId: AccountId, val withdraw: Money, override val transactionId: TransactionId) :
    AccountCommand
