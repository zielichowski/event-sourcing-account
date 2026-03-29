package com.javacaptain.eventsourcingaccount.infrastructure

import java.util.UUID

data class SerializedEvent(
    val accountId: UUID,
    val sequenceNumber: Long,
    val transactionId: UUID,
    val eventType: EventType,
    val payload: ByteArray? = null,
)
