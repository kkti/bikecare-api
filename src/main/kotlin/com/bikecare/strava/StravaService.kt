package com.bikecare.strava

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

data class StravaTokenResponse(
    val token_type: String,
    val access_token: String,
    val expires_at: Long,
    val expires_in: Int,
    val refresh_token: String,
    val athlete: Map<String, Any>?
)

@Service
class StravaService(
    @Value("\${strava.client-id:}") private val clientId: String,
    @Value("\${strava.client-secret:}") private val clientSecret: String
) {
    private val client = RestClient.create("https://www.strava.com")

    fun exchangeCode(code: String): StravaTokenResponse {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw IllegalStateException("Missing STRAVA_CLIENT_ID/STRAVA_CLIENT_SECRET")
        }
        val form = LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("code", code)
            add("grant_type", "authorization_code")
        }
        return client.post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(StravaTokenResponse::class.java)!!
    }
}
