package com.bikecare.strava

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/strava")
class StravaController(
    @Value("\${strava.client-id:}") private val clientId: String,
    @Value("\${strava.redirect-uri:}") private val redirectUri: String
) {

    @GetMapping("/oauth/url")
    fun oauthUrl(): Map<String, String> {
        if (clientId.isBlank() || redirectUri.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED,
                "Configure strava.client-id and strava.redirect-uri"
            )
        }
        val url = UriComponentsBuilder.fromHttpUrl("https://www.strava.com/oauth/authorize")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("approval_prompt", "auto")
            .queryParam("scope", "read,activity:read_all")
            .build()
            .toUriString()
        return mapOf("url" to url)
    }

    @GetMapping("/oauth/callback")
    fun oauthCallback(
        @RequestParam code: String,
        @RequestParam(required = false) scope: String?
    ): ResponseEntity<String> {
        // Skeleton – výměnu code -> token doplníme v další iteraci
        return ResponseEntity
            .status(HttpStatus.NOT_IMPLEMENTED)
            .body("Received code=$code (scope=$scope) – token exchange not implemented yet")
    }
}
