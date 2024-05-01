package com.javacaptain.eventsourcingaccount.web

import com.javacaptain.eventsourcingaccount.infrastructure.TestContainersExecutionListener
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestExecutionListeners
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners(
    listeners = [TestContainersExecutionListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
)
internal class AccountControllerTest {
    companion object {
        private const val BASE_URI = "http://localhost"
    }

    @LocalServerPort
    var port: Int? = null

    @Test
    fun `should open account`() {
        val openAccountRequest = OpenAccountRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        openAccountRequest(openAccountRequest)
            .then()
            .statusCode(201)
    }

    @Test
    fun `should require valid UUID for transactionId`() {
        val openAccountRequest = OpenAccountRequest("random", "abcd")
        openAccountRequest(openAccountRequest)
            .then()
            .statusCode(400)
            .body("errors", hasItems("Transaction id should be in UUID format", "Owner id should be in UUID format"))
    }

    @Test
    fun `should deposit money`() {
        val openAccountRequest = OpenAccountRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val depositUrl =
            openAccountRequest(openAccountRequest)
                .getHeader("Location")

        val depositMoney = DepositMoneyRequest(10, UUID.randomUUID().toString())

        depositRequest(depositMoney, depositUrl)
    }

    @Test
    fun `should conflict on duplicated transaction id`() {
        val openAccountRequest = OpenAccountRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val depositUrl =
            openAccountRequest(openAccountRequest)
                .getHeader("Location")
        val transactionId = UUID.randomUUID().toString()
        val depositMoney = DepositMoneyRequest(10, transactionId)

        depositRequest(depositMoney, depositUrl)

        given()
            .baseUri("$BASE_URI:$port")
            .contentType(ContentType.JSON)
            .body(depositMoney)
            .`when`().put("$depositUrl/deposits")
            .then()
            .statusCode(409)
    }

    @Test
    fun `should withdraw money`() {
        val openAccountRequest = OpenAccountRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val accountUrl = openAccountRequest(openAccountRequest).getHeader("Location")

        val depositMoney = DepositMoneyRequest(10, UUID.randomUUID().toString())

        depositRequest(depositMoney, accountUrl)

        val withdrawMoney = WithdrawMoneyRequest(9, UUID.randomUUID().toString())

        withdrawRequest(withdrawMoney, accountUrl)
    }

    @Test
    fun `should get correct account state`() {
        val openAccountRequest = OpenAccountRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val accountUrl = openAccountRequest(openAccountRequest).getHeader("Location")
        depositRequest(DepositMoneyRequest(10, UUID.randomUUID().toString()), accountUrl)
        depositRequest(DepositMoneyRequest(10, UUID.randomUUID().toString()), accountUrl)
        withdrawRequest(WithdrawMoneyRequest(5, UUID.randomUUID().toString()), accountUrl)

        given()
            .baseUri("$BASE_URI:$port")
            .contentType(ContentType.JSON)
            .`when`().get(accountUrl)
            .then()
            .statusCode(200)
            .body("balance", equalTo(15))
    }

    // more tests ...

    private fun depositRequest(
        depositMoney: DepositMoneyRequest,
        accountUrl: String?
    ) {
        given()
            .baseUri("$BASE_URI:$port")
            .contentType(ContentType.JSON)
            .body(depositMoney)
            .`when`().put("$accountUrl/deposits")
            .then()
            .statusCode(202)
    }

    private fun withdrawRequest(
        withdrawMoney: WithdrawMoneyRequest,
        accountUrl: String?
    ) {
        given()
            .baseUri("$BASE_URI:$port")
            .contentType(ContentType.JSON)
            .body(withdrawMoney)
            .`when`().put("$accountUrl/withdraws")
            .then()
            .statusCode(202)
    }

    private fun openAccountRequest(openAccountRequest: OpenAccountRequest): Response =
        given()
            .baseUri("$BASE_URI:$port")
            .contentType(ContentType.JSON)
            .body(openAccountRequest)
            .`when`().post("/accounts")
}
