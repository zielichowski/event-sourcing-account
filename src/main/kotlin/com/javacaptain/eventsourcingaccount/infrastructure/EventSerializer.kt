package com.javacaptain.eventsourcingaccount.infrastructure

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.daveanthonythomas.moshipack.MoshiPack
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId
import com.javacaptain.eventsourcingaccount.domain.AccountEvent
import com.javacaptain.eventsourcingaccount.domain.AccountOpened
import com.javacaptain.eventsourcingaccount.domain.MoneyDeposited
import com.javacaptain.eventsourcingaccount.domain.MoneyWithdrawn
import java.util.*

internal interface EventSerializer {
    fun serialize(accountEvent: AccountEvent): SerializedEvent

    fun deserialize(serializedEvent: SerializedEvent): Either<DomainError, AccountEvent>
}

class MessagePackSerializer(
    private val moshiPack: MoshiPack,
) : EventSerializer {
    override fun serialize(accountEvent: AccountEvent): SerializedEvent {
        return when (accountEvent) {
            is AccountOpened ->
                SerializedEvent(
                    accountEvent.accountId.value,
                    accountEvent.version,
                    accountEvent.transactionId.value,
                    EventType.ACCOUNT_OPENED,
                    moshiPack.packToByteArray(
                        AccountOpenedPayload(
                            ownerId = accountEvent.ownerId.value.toString(),
                            initBalance = accountEvent.initialBalance.value,
                        ),
                    ),
                )

            is MoneyDeposited ->
                SerializedEvent(
                    accountEvent.accountId.value,
                    accountEvent.version,
                    accountEvent.transactionId.value,
                    EventType.MONEY_DEPOSITED,
                    moshiPack.packToByteArray(
                        MoneyDepositedPayload(
                            balance = accountEvent.balance.value,
                            amountDeposited = accountEvent.amountDeposited.value,
                        ),
                    ),
                )

            is MoneyWithdrawn ->
                SerializedEvent(
                    accountEvent.accountId.value,
                    accountEvent.version,
                    accountEvent.transactionId.value,
                    EventType.MONEY_WITHDRAWN,
                    moshiPack.packToByteArray(
                        MoneyWithdrawnPayload(
                            balance = accountEvent.balance.value,
                            amountWithdrawn = accountEvent.amountWithdrawn.value,
                        ),
                    ),
                )
        }
    }

    override fun deserialize(serializedEvent: SerializedEvent): Either<DomainError, AccountEvent> {
        return either {
            when (serializedEvent.eventType) {
                EventType.ACCOUNT_OPENED -> {
                    ensure(
                        serializedEvent.payload != null,
                    ) { DomainError.EmptyPayloadError("Unable to deserialize event=$serializedEvent, due to the null payload") }
                    val payload = moshiPack.unpack<AccountOpenedPayload>(serializedEvent.payload)
                    AccountOpened(
                        AccountId(serializedEvent.accountId),
                        serializedEvent.sequenceNumber,
                        TransactionId(serializedEvent.transactionId),
                        OwnerId(UUID.fromString(payload.ownerId)),
                        Money(payload.initBalance).bind(),
                    )
                }

                EventType.MONEY_DEPOSITED -> {
                    ensure(
                        serializedEvent.payload != null,
                    ) { DomainError.EmptyPayloadError("Unable to deserialize event=$serializedEvent, due to the null payload") }
                    val payload = moshiPack.unpack<MoneyDepositedPayload>(serializedEvent.payload)
                    MoneyDeposited(
                        AccountId(serializedEvent.accountId),
                        serializedEvent.sequenceNumber,
                        TransactionId(serializedEvent.transactionId),
                        Money(payload.amountDeposited).bind(),
                        Money(payload.balance).bind(),
                    )
                }

                EventType.MONEY_WITHDRAWN -> {
                    ensure(
                        serializedEvent.payload != null,
                    ) { DomainError.EmptyPayloadError("Unable to deserialize event=$serializedEvent, due to the null payload") }
                    val payload = moshiPack.unpack<MoneyWithdrawnPayload>(serializedEvent.payload)
                    MoneyWithdrawn(
                        AccountId(serializedEvent.accountId),
                        serializedEvent.sequenceNumber,
                        TransactionId(serializedEvent.transactionId),
                        Money(payload.amountWithdrawn).bind(),
                        Money(payload.balance).bind(),
                    )
                }
            }
        }
    }
}

internal data class AccountOpenedPayload(
    val ownerId: String,
    val initBalance: Long,
)

internal data class MoneyDepositedPayload(
    val balance: Long,
    val amountDeposited: Long,
)

internal data class MoneyWithdrawnPayload(
    val balance: Long,
    val amountWithdrawn: Long,
)

enum class EventType {
    ACCOUNT_OPENED,
    MONEY_DEPOSITED,
    MONEY_WITHDRAWN,
}
