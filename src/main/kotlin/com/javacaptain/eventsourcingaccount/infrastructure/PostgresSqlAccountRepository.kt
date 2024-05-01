package com.javacaptain.eventsourcingaccount.infrastructure

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.domain.AccountEvent
import org.postgresql.util.PSQLException
import java.io.IOException
import java.io.UncheckedIOException
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import javax.sql.DataSource

internal class PostgresSqlAccountRepository(
    private val dataSource: DataSource,
    private val eventSerializer: EventSerializer,
) : AccountRepository {
    companion object {
        private const val APPEND_EVENT_SQL =
            "INSERT INTO Event(aggregateId, sequenceNumber, transactionId, eventType, payload) VALUES(?, ?, ?, ?, ?)"

        private const val SELECT_EVENTS_SQL =
            "SELECT sequenceNumber, transactionId, eventType, payload  FROM Event WHERE aggregateId = ? AND sequenceNumber > ? " +
                "ORDER BY sequenceNumber ASC"

        private const val UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE = "23505"
    }

    override fun getEvents(accountId: AccountId): Either<DomainError, Sequence<AccountEvent>> {
        return Either.catchOrThrow<SQLException, List<SerializedEvent>> {
            dataSource.connection.use {
                it.prepareStatement(SELECT_EVENTS_SQL).use { prepareStatement ->
                    prepareStatement.setObject(1, accountId.value)
                    prepareStatement.setLong(2, 0)

                    val serializedEvents =
                        prepareStatement.executeQuery().use { resultSet ->
                            val eventPayloads: MutableList<SerializedEvent> = mutableListOf()
                            while (resultSet.next()) {
                                eventPayloads.add(
                                    SerializedEvent(
                                        accountId.value,
                                        resultSet.getLong(1),
                                        resultSet.getObject(2, UUID::class.java),
                                        EventType.valueOf(resultSet.getString(3)),
                                        resultSet.getBytes(4),
                                    ),
                                )
                            }
                            eventPayloads
                        }
                    serializedEvents
                }
            }
        }
            .mapLeft { throw UncheckedIOException(IOException(it)) }
            .flatMap { deserialize(it) }
    }

    private fun deserialize(serializedEvents: List<SerializedEvent>): Either<DomainError, Sequence<AccountEvent>> {
        return either {
            serializedEvents.map {
                eventSerializer.deserialize(it).bind()
            }.asSequence()
        }
    }

    override fun append(accountEvent: AccountEvent): Either<DomainError, AccountEvent> {
        val serializedEvent = eventSerializer.serialize(accountEvent)
        return Either.catch {
            dataSource.connection.use {
                it.prepareStatement(APPEND_EVENT_SQL).use { prepareStatement ->
                    prepareStatement.setObject(1, serializedEvent.accountId)
                    prepareStatement.setLong(2, serializedEvent.sequenceNumber)
                    prepareStatement.setObject(3, serializedEvent.transactionId)
                    prepareStatement.setString(4, serializedEvent.eventType.name)
                    prepareStatement.setBytes(5, serializedEvent.payload)
                    prepareStatement.executeUpdate()
                }
            }
        }.mapLeft {
            when (it) {
                is PSQLException ->
                    if (it.sqlState.equals(UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE)) {
                        DomainError.ConcurrentModificationError(
                            "Duplicated event=$accountEvent",
                        )
                    } else {
                        throw UncheckedIOException(IOException(it))
                    }

                is SQLIntegrityConstraintViolationException -> DomainError.ConcurrentModificationError("Duplicated event=$accountEvent")
                else -> throw UncheckedIOException(IOException(it))
            }
        }.map {
            accountEvent
        }
    }
}
