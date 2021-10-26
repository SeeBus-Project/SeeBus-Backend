package com.opensource.seebus

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class SeebusApplication

fun main(args: Array<String>) {
    runApplication<SeebusApplication>(*args)
}
