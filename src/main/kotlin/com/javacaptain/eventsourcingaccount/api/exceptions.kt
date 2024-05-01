package com.javacaptain.eventsourcingaccount.api

sealed interface DomainError {
    val message: String

    data class ConcurrentModificationError(override val message: String) : DomainError

    data class DuplicatedTransactionError(override val message: String) : DomainError

    data class AccountAlreadyOpenedError(override val message: String) : DomainError

    data class AccountNotInitializedError(override val message: String = "Account has not been initialized") : DomainError

    data class InsufficientBalanceError(override val message: String) : DomainError

    data class EmptyTransactionId(override val message: String = "Transaction id should not be empty") : DomainError

    data class EmptyOwnerId(override val message: String = "Owner id should not be empty") : DomainError

    data class InvalidTransactionIdFormat(override val message: String = "Transaction id should be in UUID format") : DomainError

    data class InvalidOwnerIdFormat(override val message: String = "Owner id should be in UUID format") : DomainError

    data class InvalidAccountIdFormat(override val message: String = "Account id should be in UUID format") : DomainError

    data class EmptyPayloadError(override val message: String) : DomainError

    data class MoneyError(override val message: String) : DomainError
}
