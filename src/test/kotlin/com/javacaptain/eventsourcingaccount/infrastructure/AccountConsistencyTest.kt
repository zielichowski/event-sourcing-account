package com.javacaptain.eventsourcingaccount.infrastructure

import com.daveanthonythomas.moshipack.MoshiPack
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DepositMoney
import com.javacaptain.eventsourcingaccount.api.Money
import com.javacaptain.eventsourcingaccount.api.OpenAccount
import com.javacaptain.eventsourcingaccount.api.OwnerId
import com.javacaptain.eventsourcingaccount.api.TransactionId
import com.javacaptain.eventsourcingaccount.domain.AccountCommandHandler
import com.javacaptain.eventsourcingaccount.domain.AccountEventHandler
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestExecutionListeners
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import javax.sql.DataSource

@TestExecutionListeners(
    listeners = [TestContainersExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
)
@SpringBootTest
internal class AccountConsistencyTest {
    @Autowired
    private lateinit var dataSource: DataSource
    val accountId = AccountId(UUID.randomUUID())
    val ownerId = OwnerId(UUID.randomUUID())
    val oneDollar = Money(1).getOrNull()!!

    @Test
    fun `account should stay consistent with concurrent deposits`() {
        val accountEventHandler = AccountEventHandler()
        val accountCommandHandler =
            AccountCommandHandler(
                accountEventHandler,
                PostgresSqlAccountRepository(dataSource, MessagePackSerializer(MoshiPack())),
            )

        val executor = Executors.newFixedThreadPool(10)

        accountCommandHandler.handle(
            OpenAccount(
                accountId,
                ownerId,
                TransactionId(UUID.randomUUID()),
            ),
        )

        val operationCount = 20
        val threadCount = 10
        // TODo cleanup

        // Old school java 1.6
        for (i in 0 until operationCount) {
            val latch = CountDownLatch(threadCount)
            for (j in 0 until threadCount) {
                executor.submit {
                    while (true) {
                        val handle =
                            accountCommandHandler.handle(
                                DepositMoney(
                                    accountId,
                                    oneDollar,
                                    TransactionId(UUID.randomUUID()),
                                ),
                            )
                        if (handle.isRight()) {
                            latch.countDown()
                            break
                        }
                    }
                }
            }
            latch.await()
        }

        dataSource.connection.use {
            val prepareStatement = it.prepareStatement("SELECT count(*) from event where aggregateId = ?")
            prepareStatement.setObject(1, accountId.value)
            val executeQuery = prepareStatement.executeQuery()
            while (executeQuery.next()) {
                val numberOfEvents = executeQuery.getInt(1)
                Assertions.assertEquals(1 + operationCount * threadCount, numberOfEvents) // open account event + deposits
            }
        }
    }
}
