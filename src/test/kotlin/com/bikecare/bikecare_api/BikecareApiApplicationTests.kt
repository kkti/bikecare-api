package com.bikecare

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.flyway.enabled=false"])
class BikecareApiApplicationTests {
    @Test
    fun contextLoads() {
        // jen ověř, že Spring kontext naběhne
    }
}
