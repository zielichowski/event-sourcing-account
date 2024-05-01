package simulations

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.header
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import io.gatling.javaapi.http.HttpProtocolBuilder
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * We take 2 steps in the load test scenario.
 * 1. We create multiple accounts
 * 2. We send multiple concurrent deposit request to previously created accounts
 */
class AccountLoadTest : Simulation() {
    private val httpConf: HttpProtocolBuilder =
        http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")

    private val createAccountRequest =
        """
        {
            "transactionId": "${UUID.randomUUID()}",
            "ownerId": "${UUID.randomUUID()}"
        }
        """.trimIndent()

    private val accountLinks = mutableListOf<String>()

    private val scn =
        scenario("Create account scenario")
            .exec(
                http("Create account")
                    .post("/accounts")
                    .body(StringBody(createAccountRequest)).asJson()
                    .check(status().`is`(201))
                    .check(header("Location").saveAs("accountLink")),
            )
            .exec { session ->
                val accountLink = session.get<String>("accountLink")
                accountLinks.add(accountLink!!)
                session
            }

    private val scn1 =
        scenario("Deposit money scenario")
            .exec { session ->
                val set = session.set("accountLink", accountLinks.random())
                val set1 = set.set("trxId", UUID.randomUUID().toString())
                set1
            }
            .exec(
                http("Deposit money")
                    .put("#{accountLink}/deposits")
                    .body(
                        StringBody(
                            """
                            {
                                "transactionId": "#{trxId}",
                                "money": "10"
                            }
                            """.trimIndent(),
                        ),
                    ).asJson()
                    .check(status().`is`(202)),
            )

    init {
        val injectionStep = constantUsersPerSec(40.0).during(2.seconds.inWholeSeconds)

        setUp(
            scn.injectOpen(injectionStep).protocols(httpConf)
                .andThen(
                    scn1.injectOpen(
                        rampUsersPerSec(30.0).to(100.0).during(Duration.ofMinutes(20)), // 6
                    ).protocols(httpConf),
                ),
        )
    }
}
