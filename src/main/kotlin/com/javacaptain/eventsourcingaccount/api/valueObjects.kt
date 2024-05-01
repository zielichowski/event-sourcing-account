package com.javacaptain.eventsourcingaccount.api

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import java.util.*

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class OwnerId(val value: UUID)

@JvmInline
value class TransactionId(val value: UUID)

/**
 * A very simplified version, without currency.
 */
@JvmInline
value class Money private constructor(val value: Long) {
    fun add(money: Money): Money {
        return Money(this.value + money.value)
    }

    fun subtract(money: Money): Money {
        return Money(this.value - money.value)
    }

    companion object {
        operator fun invoke(value: Long): Either<DomainError.MoneyError, Money> =
            either {
                ensure(value >= 0) { DomainError.MoneyError("Amount cannot be less than zero, currentValue=$value") }
                Money(value)
            }
    }
}
