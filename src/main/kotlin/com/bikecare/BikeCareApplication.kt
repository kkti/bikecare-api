package com.bikecare

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BikeCareApplication

fun main(args: Array<String>) {
    runApplication<BikeCareApplication>(*args)
}
