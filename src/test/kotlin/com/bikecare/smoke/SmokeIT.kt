package com.bikecare.smoke

import com.bikecare.test.LocalIntegrationTestBase
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class SmokeIT : LocalIntegrationTestBase() {

    @Test
    fun register_and_login_flow() {
        val email = "it+" + System.currentTimeMillis() + "@example.com"

        // register
        given()
            .contentType(ContentType.JSON)
            .body("""{"email":"$email","password":"test1234"}""")
        .`when`()
            .post("/api/auth/register")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(201)))

        // login -> vytáhni token
        val token: String =
            given()
                .contentType(ContentType.JSON)
                .body("""{"email":"$email","password":"test1234"}""")
            .`when`()
                .post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract()
                .path("token")

        // GET /api/bikes s bearerem
        given()
            .header("Authorization", "Bearer $token")
        .`when`()
            .get("/api/bikes")
        .then()
            .statusCode(200)
    }
}
