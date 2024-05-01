package com.javacaptain.eventsourcingaccount.infrastructure

import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class TestContainersExecutionListener : TestExecutionListener {
    companion object {
        private const val POSTGRES_IMAGE_NAME = "postgres:13.11"
        private const val POSTGRES_DB_NAME = "test"

        private val postgreContainer =
            PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE_NAME)).withUsername("test").withPassword("test")
    }

    @Throws(Exception::class)
    override fun beforeTestClass(testContext: TestContext) {
        if (!postgreContainer.isRunning) {
            postgreContainer.start()
        }
        System.setProperty(
            "spring.datasource.url",
            "jdbc:postgresql://${postgreContainer.host}:${postgreContainer.firstMappedPort}/$POSTGRES_DB_NAME",
        )
        System.setProperty(
            "spring.datasource.username",
            "test",
        )
        System.setProperty(
            "spring.datasource.password",
            "test",
        )
    }
}
