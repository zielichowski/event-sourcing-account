package com.javacaptain.eventsourcingaccount.domain

import com.daveanthonythomas.moshipack.MoshiPack
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DepositMoney
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OpenAccount
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId
import com.javacaptain.eventsourcingaccount.api.WithdrawMoney
import com.javacaptain.eventsourcingaccount.infrastructure.MessagePackSerializer
import com.javacaptain.eventsourcingaccount.infrastructure.PostgresSqlAccountRepository
import com.javacaptain.eventsourcingaccount.infrastructure.TestContainersExecutionListener
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestExecutionListeners
import java.util.*
import javax.sql.DataSource

@TestExecutionListeners(
    listeners = [TestContainersExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
)
@SpringBootTest
internal class AccountCommandHandlerTest {
    @Autowired
    private lateinit var dataSource: DataSource
    val accountId = AccountId(UUID.randomUUID())
    val ownerId = OwnerId(UUID.randomUUID())
    val initBalance = Money(0).getOrNull()!!

    @Nested
    @DisplayName("When account is not initialized")
    inner class AccountNotInitialized {
        private val accountCommandHandler =
            AccountCommandHandler(
                AccountEventHandler(),
                PostgresSqlAccountRepository(dataSource, MessagePackSerializer(MoshiPack())),
            )

        @Test
        fun `then open account command should open a new account`() {
            val transactionId = TransactionId(UUID.randomUUID())
            val event =
                accountCommandHandler.handle(
                    OpenAccount(
                        accountId = accountId,
                        ownerId = ownerId,
                        transactionId = transactionId,
                    ),
                )

            val expectedEvent = AccountOpened(accountId, 1, transactionId, ownerId, initBalance)

            Assertions.assertEquals(expectedEvent, event.getOrNull())
        }

        @Test
        fun `then deposit command should fail`() {
            val transactionId = TransactionId(UUID.randomUUID())
            val domainException =
                accountCommandHandler.handle(
                    DepositMoney(
                        accountId = accountId,
                        deposit = initBalance,
                        transactionId = transactionId,
                    ),
                ).leftOrNull()

            Assertions.assertTrue { domainException is DomainError.AccountNotInitializedError }
        }

        @Test
        fun `then withdraw command should fail`() {
            val transactionId = TransactionId(UUID.randomUUID())
            val domainException =
                accountCommandHandler.handle(
                    WithdrawMoney(
                        accountId,
                        initBalance,
                        transactionId,
                    ),
                ).leftOrNull()

            Assertions.assertTrue { domainException is DomainError.AccountNotInitializedError }
        }
    }

    @Nested
    @DisplayName("When account is initialized")
    inner class AccountInitialized {
        private val accountCommandHandler =
            AccountCommandHandler(
                AccountEventHandler(),
                PostgresSqlAccountRepository(dataSource, MessagePackSerializer(MoshiPack())),
            )
        private val tenUsd = Money(10).getOrNull()!!

        @Test
        fun `then deposit command should increase balance`() {
            openAccount(TransactionId(UUID.randomUUID()))

            val transactionId = TransactionId(UUID.randomUUID())
            val event =
                accountCommandHandler.handle(
                    DepositMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                )
            val expectedEvent = MoneyDeposited(accountId, 2, transactionId, tenUsd, initBalance)

            Assertions.assertEquals(expectedEvent, event.getOrNull())
        }

        @Test
        fun `then withdraw command should be applied when balance is sufficient`() {
            openAccount(TransactionId(UUID.randomUUID()))
            accountCommandHandler.handle(
                DepositMoney(
                    accountId,
                    tenUsd.add(tenUsd),
                    TransactionId(UUID.randomUUID()),
                ),
            )
            val transactionId = TransactionId(UUID.randomUUID())
            val event =
                accountCommandHandler.handle(
                    WithdrawMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                )
            val expectedEvent = MoneyWithdrawn(accountId, 3, transactionId, tenUsd, Money(20).getOrNull()!!)

            Assertions.assertEquals(expectedEvent, event.getOrNull())
        }

        @Test
        fun `then withdraw command should not be applied when balance is insufficient `() {
            openAccount(TransactionId(UUID.randomUUID()))

            val transactionId = TransactionId(UUID.randomUUID())
            val domainException =
                accountCommandHandler.handle(
                    WithdrawMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                )

            Assertions.assertTrue { domainException.leftOrNull() is DomainError.InsufficientBalanceError }
        }

        @Test
        fun `then deposit command should be idempotent`() {
            openAccount(TransactionId(UUID.randomUUID()))

            val transactionId = TransactionId(UUID.randomUUID())
            val event =
                accountCommandHandler.handle(
                    DepositMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                )
            val expectedEvent = MoneyDeposited(accountId, 2, transactionId, tenUsd, initBalance)

            Assertions.assertEquals(expectedEvent, event.getOrNull())

            val domainException =
                accountCommandHandler.handle(
                    DepositMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                ).leftOrNull()

            Assertions.assertTrue { domainException is DomainError.DuplicatedTransactionError }
        }

        @Test
        fun `then withdraw command should be idempotent`() {
            openAccount(TransactionId(UUID.randomUUID()))
            accountCommandHandler.handle(
                DepositMoney(
                    accountId,
                    tenUsd.add(tenUsd),
                    TransactionId(UUID.randomUUID()),
                ),
            )
            val transactionId = TransactionId(UUID.randomUUID())
            val event =
                accountCommandHandler.handle(
                    WithdrawMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                )
            val expectedEvent = MoneyWithdrawn(accountId, 3, transactionId, tenUsd, Money(20).getOrNull()!!)

            Assertions.assertEquals(expectedEvent, event.getOrNull())

            val domainException =
                accountCommandHandler.handle(
                    WithdrawMoney(
                        accountId,
                        tenUsd,
                        transactionId,
                    ),
                ).leftOrNull()
            Assertions.assertTrue { domainException is DomainError.DuplicatedTransactionError }
        }

        private fun openAccount(transactionId: TransactionId) =
            accountCommandHandler.handle(
                OpenAccount(
                    accountId,
                    ownerId,
                    transactionId,
                ),
            )
    }
}
