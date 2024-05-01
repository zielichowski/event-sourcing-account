package com.javacaptain.eventsourcingaccount.web

import arrow.core.merge
import com.javacaptain.eventsourcingaccount.api.AccountCommand
import com.javacaptain.eventsourcingaccount.api.AccountId
import com.javacaptain.eventsourcingaccount.api.AccountQuery
import com.javacaptain.eventsourcingaccount.api.DepositMoney
import com.javacaptain.eventsourcingaccount.api.DomainError
import com.javacaptain.eventsourcingaccount.api.OpenAccount
import com.javacaptain.eventsourcingaccount.api.WithdrawMoney
import com.javacaptain.eventsourcingaccount.domain.AccountCommandHandler
import com.javacaptain.eventsourcingaccount.domain.AccountQueryHandler
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.*

@RestController(value = "api/v1")
internal class AccountController(
    private val commandHandler: AccountCommandHandler,
    private val queryHandler: AccountQueryHandler
) {
    @PostMapping("/accounts")
    fun openAccount(
        @RequestBody openAccountRequest: OpenAccountRequest,
    ): ResponseEntity<out Any> {
        return openAccountRequest.validate()
            .map { openAccount -> mapCommand(openAccount) }
            .mapLeft {
                ResponseEntity.badRequest()
                    .body(ApiErrorResponse(it.map { domainException -> domainException.message }))
            }.merge()
    }

    @PutMapping("/accounts/{accountId}/deposits")
    fun depositMoney(
        @RequestBody depositMoneyRequest: DepositMoneyRequest,
        @PathVariable accountId: String,
    ): ResponseEntity<out Any> {
        return depositMoneyRequest.validate(accountId)
            .map { depositMoney ->
                mapCommand(depositMoney)
            }
            .mapLeft {
                ResponseEntity.badRequest()
                    .body(ApiErrorResponse(it.map { domainException -> domainException.message }))
            }.merge()
    }

    @PutMapping("/accounts/{accountId}/withdraws")
    fun withdrawMoney(
        @RequestBody withdrawMoneyRequest: WithdrawMoneyRequest,
        @PathVariable accountId: String,
    ): ResponseEntity<out Any> {
        return withdrawMoneyRequest.validate(accountId)
            .map { withdrawMoney -> mapCommand(withdrawMoney) }
            .mapLeft {
                ResponseEntity.badRequest()
                    .body(ApiErrorResponse(it.map { domainException -> domainException.message }))
            }.merge()
    }

    @GetMapping("/accounts/{accountId}")
    fun getAccount(@PathVariable accountId: String): ResponseEntity<out Any> {
        return queryHandler.getAccount(AccountQuery(AccountId(UUID.fromString(accountId))))
            .map { ResponseEntity.ok(it) }
            .mapLeft { ResponseEntity.badRequest().build<Any>() }.merge() // TODO error handling
    }

    private fun mapCommand(accountCommand: AccountCommand): ResponseEntity<out Any> {
        return when (accountCommand) {
            is DepositMoney, is WithdrawMoney ->
                commandHandler.handleWithRetries(accountCommand)
                    .map { ResponseEntity.accepted().build<Any>() }
                    .mapLeft {
                        when (it) {
                            is DomainError.DuplicatedTransactionError ->
                                ResponseEntity.status(409).body(ApiErrorResponse(listOf(it.message))) // conflict
                            else -> ResponseEntity.badRequest().body(ApiErrorResponse(listOf(it.toString())))
                        }
                    }.merge()

            is OpenAccount ->
                commandHandler.handle(accountCommand)
                    .map { ResponseEntity.created(URI("/accounts/${it.accountId.value}")).build<Any>() }
                    .mapLeft {
                        when (it) {
                            is DomainError.DuplicatedTransactionError ->
                                ResponseEntity.status(409).body(ApiErrorResponse(listOf(it.message))) // conflict
                            is DomainError.AccountNotInitializedError -> ResponseEntity.notFound().build()
                            else -> ResponseEntity.badRequest().body(ApiErrorResponse(listOf(it.toString())))
                        }
                    }.merge()
        }
    }
}
