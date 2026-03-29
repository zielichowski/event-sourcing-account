package com.javacaptain.eventsourcingaccount.domain

import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OwnerId

sealed interface AccountState {
    data class OpenedAccount(
        val accountId: AccountId,
        val ownerId: OwnerId,
        val balance: Money,
        val version: Long,
    ) : AccountState

    data object NotInitialized : AccountState
}
