package com.javacaptain.eventsourcingaccount.infrastructure

import com.javacaptain.eventsourcingaccount.domain.AccountEvent
import com.javacaptain.eventsourcingaccount.domain.AccountOpened
import com.javacaptain.eventsourcingaccount.domain.MoneyDeposited
import com.javacaptain.eventsourcingaccount.domain.MoneyWithdrawn
import org.springframework.kafka.core.KafkaTemplate

internal class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    private val accountEventsTopic: String,
) : EventPublisher {
    override fun publish(accountEvent: AccountEvent) {
        kafkaTemplate.send(accountEventsTopic, accountEvent.accountId.value.toString(), serialize(accountEvent)).join()
    }

    // TODO separate mapper
    private fun serialize(accountEvent: AccountEvent): ByteArray {
        val protoEvent = when (accountEvent) {
            is AccountOpened ->
                com.account.v1.AccountEvent.newBuilder()
                    .setAccountOpened(accountEvent.toProto())
                    .build()

            is MoneyDeposited ->
                com.account.v1.AccountEvent.newBuilder()
                    .setMoneyDeposited(accountEvent.toProto())
                    .build()

            is MoneyWithdrawn ->
                com.account.v1.AccountEvent.newBuilder()
                    .setMoneyWithdrawn(accountEvent.toProto())
                    .build()
        }

        return protoEvent.toByteArray()
    }

    fun AccountOpened.toProto(): com.account.v1.AccountOpened =
        com.account.v1.AccountOpened.newBuilder()
            .setAccountId(accountId.value.toString())
            .setTransactionId(transactionId.value.toString())
            .setVersion(version)
            .setOwnerId(ownerId.value.toString())
            .setInitialBalance(initialBalance.value)
            .build()

    fun MoneyDeposited.toProto(): com.account.v1.MoneyDeposited =
        com.account.v1.MoneyDeposited.newBuilder()
            .setAccountId(accountId.value.toString())
            .setTransactionId(transactionId.value.toString())
            .setVersion(version)
            .setAmountDeposited(amountDeposited.value)
            .setBalance(balance.value)
            .build()

    fun MoneyWithdrawn.toProto(): com.account.v1.MoneyWithdrawn =
        com.account.v1.MoneyWithdrawn.newBuilder()
            .setAccountId(accountId.value.toString())
            .setTransactionId(transactionId.value.toString())
            .setVersion(version)
            .setAmountWithdrawn(amountWithdrawn.value)
            .setBalance(balance.value)
            .build()
}