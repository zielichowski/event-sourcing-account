package com.javacaptain.eventsourcingaccount.api

import java.util.UUID

data class Account(
    val accountId: UUID,
    val ownerId: UUID,
    val balance: Long,
)

data class AccountQuery(
    val accountId: AccountId,
)