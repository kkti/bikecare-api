package com.bikecare.bikecomponent

import com.bikecare.test.LocalIntegrationTestBase
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ComponentStatusIT : LocalIntegrationTestBase() {

    @Test
    fun end_to_end_statuses_warn_critical_expired() {
        val email = "status+" + System.currentTimeMillis() + "@example.com"

        // 1) register (200 nebo 201)
        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"test1234"}""")
        .`when`()
            .post("/api/auth/register")
        .then()
            .log().ifValidationFails()
            .statusCode(anyOf(equalTo(200), equalTo(201)))

        // 2) login → token (200)
        val token: String =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"test1234"}""")
            .`when`()
                .post("/api/auth/login")
            .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .path("token")

        // 3) create bike (200 nebo 201)
        val bikeId: Int =
            given()
                .header("Authorization", "Bearer $token")
                .contentType(ContentType.JSON)
                .body("""{"name":"Defy","brand":"Giant","model":"Advanced"}""")
            .`when`()
                .post("/api/bikes")
            .then()
                .log().ifValidationFails()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .extract()
                .path("id")

        // 4) create component s lifespanOverride=2500, installed=3600 (201)
        val compId: Int =
            given()
                .header("Authorization", "Bearer $token")
                .contentType(ContentType.JSON)
                .body("""{"typeKey":"chain","position":"REAR","installedOdometerKm":3600,"lifespanOverride":2500}""")
            .`when`()
                .post("/api/bikes/$bikeId/components")
            .then()
                .log().ifValidationFails()
                .statusCode(201)
                .extract()
                .path("id")

        // Explicitní prahy, ať je test nezávislý na YAML: warn=75, critical=95

        // WARN @ 5600 (80 %)
        given()
            .header("Authorization", "Bearer $token")
        .`when`()
            .get("/api/bikes/$bikeId/components/$compId?currentOdometerKm=5600&warnAt=75&criticalAt=95")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("status", equalTo("WARN"))

        // CRITICAL @ 6000 (96 %)
        given()
            .header("Authorization", "Bearer $token")
        .`when`()
            .get("/api/bikes/$bikeId/components/$compId?currentOdometerKm=6000&warnAt=75&criticalAt=95")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("status", equalTo("CRITICAL"))

        // EXPIRED @ 6100 (100 %, remaining==0)
        given()
            .header("Authorization", "Bearer $token")
        .`when`()
            .get("/api/bikes/$bikeId/components/$compId?currentOdometerKm=6100&warnAt=75&criticalAt=95")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("status", equalTo("EXPIRED"))
    }
}
