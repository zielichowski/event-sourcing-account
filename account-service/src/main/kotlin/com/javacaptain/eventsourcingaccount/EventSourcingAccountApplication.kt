package com.javacaptain.eventsourcingaccount

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventSourcingAccountApplication

fun main(args: Array<String>) {
    runApplication<EventSourcingAccountApplication>(*args)
}
