package com.bikecare.smoke

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmokeIT {

    companion object {
        @JvmStatic
        @Container
        @ServiceConnection // Spring Boot si z tohohle kontejneru nastaví spring.datasource.*
        val postgres = PostgreSQLContainer("postgres:15-alpine")
    }

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = port
    }

    @Test
    fun health_is_up() {
        RestAssured.given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/actuator/health")
            .then()
            .statusCode(200)
            .body("status", anyOf(equalTo("UP"), equalTo("UP")))
    }

    @Test
    fun register_and_login_and_list_bikes() {
        val email = "it-${System.currentTimeMillis()}@example.com"
        val password = "Passw0rd!"

        // register (nebo login – dle tvé logiky obojí vrací token)
        val token = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("email" to email, "password" to password))
            .`when`()
            .post("/api/auth/register")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(201)))
            .body("token", notNullValue())
            .extract().jsonPath().getString("token")

        // volání chráněného endpointu s Bearer tokenem
        RestAssured.given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/api/bikes")
            .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)))
    }
}
