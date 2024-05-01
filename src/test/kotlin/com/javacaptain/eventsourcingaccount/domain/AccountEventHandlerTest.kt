package com.javacaptain.eventsourcingaccount.domain

import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AccountEventHandlerTest {
    private val eventHandler = AccountEventHandler()
    private val accountId = AccountId(UUID.randomUUID())
    private val ownerId = OwnerId(UUID.randomUUID())
    private val initBalance = Money(10).getOrNull()!!
    private val tenUsd = Money(10).getOrNull()!!
    val version = 1L

    @Nested
    @DisplayName("When account is not initialized")
    inner class AccountNotInitialized {
        @Test
        fun `then open account event should open account`() {
            // given
            val sequenceOf =
                sequenceOf(AccountOpened(accountId, version, TransactionId(UUID.randomUUID()), ownerId, initBalance))
            val expectedState = AccountState.OpenedAccount(accountId, ownerId, initBalance, version)

            // when
            val state = eventHandler.state(sequenceOf)

            // then
            Assertions.assertEquals(state.getOrNull(), expectedState)
        }
        @Test
        fun `then deposit event should return domain exception`() {
            // given
            val version = 1L
            val sequenceOf =
                sequenceOf(
                    MoneyDeposited(
                        accountId = accountId,
                        version = version,
                        transactionId = TransactionId(UUID.randomUUID()),
                        amountDeposited = tenUsd,
                        balance = initBalance,
                    ),
                )

            // when
            val state = eventHandler.state(sequenceOf)

            // then
            Assertions.assertTrue { state.leftOrNull() is DomainError.AccountNotInitializedError }
        }

        @Test
        fun `then withdraw event should return domain exception`() {
            // given
            val version = 1L
            val sequenceOf =
                sequenceOf(
                    MoneyWithdrawn(
                        accountId = accountId,
                        version = version,
                        transactionId = TransactionId(UUID.randomUUID()),
                        amountWithdrawn = tenUsd,
                        balance = initBalance,
                    ),
                )

            // when
            val state = eventHandler.state(sequenceOf)

            // then
            Assertions.assertTrue { state.leftOrNull() is DomainError.AccountNotInitializedError }
        }
    }

    @Nested
    @DisplayName("When account is initialized")
    inner class AccountInitialized {
        @Test
        fun `then deposit event should increase balance `() {
            // given
            val initEvents =
                sequenceOf(
                    AccountOpened(accountId, version, TransactionId(UUID.randomUUID()), ownerId, initBalance),
                    MoneyDeposited(accountId, version.inc(), TransactionId(UUID.randomUUID()), tenUsd, initBalance),
                )
            val expectedState = AccountState.OpenedAccount(accountId, ownerId, initBalance.add(tenUsd), version.inc())

            // when
            val state = eventHandler.state(initEvents)

            // then
            Assertions.assertEquals(expectedState, state.getOrNull())
        }

        @Test
        fun `then multiple deposits should accumulate balance `() {
            // given
            val initEvents =
                sequenceOf(
                    AccountOpened(accountId, version, TransactionId(UUID.randomUUID()), ownerId, initBalance),
                    MoneyDeposited(accountId, version.inc(), TransactionId(UUID.randomUUID()), tenUsd, initBalance),
                    MoneyDeposited(
                        accountId,
                        version.inc().inc(),
                        TransactionId(UUID.randomUUID()),
                        tenUsd,
                        initBalance,
                    ),
                )
            val expectedState =
                AccountState.OpenedAccount(accountId, ownerId, initBalance.add(tenUsd).add(tenUsd), version.inc().inc())

            // when
            val state = eventHandler.state(initEvents)

            // then
            Assertions.assertEquals(expectedState, state.getOrNull())
        }

        @Test
        fun `then withdraw event should decrease balance `() {
            val money = Money(5L).getOrNull()!!
            // given
            val initEvents =
                sequenceOf(
                    AccountOpened(accountId, version, TransactionId(UUID.randomUUID()), ownerId, initBalance),
                    MoneyWithdrawn(accountId, version.inc(), TransactionId(UUID.randomUUID()), money, initBalance),
                )
            val expectedState =
                AccountState.OpenedAccount(accountId, ownerId, initBalance.subtract(money), version.inc())

            // when
            val state = eventHandler.state(initEvents)

            // then
            Assertions.assertEquals(expectedState, state.getOrNull())
        }
    }
}
