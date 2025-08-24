package com.bikecare.strava

import com.bikecare.user.AppUserRepository
import com.bikecare.security.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/strava")
class StravaController(
    @Value("\${strava.client-id:}") private val clientId: String,
    @Value("\${strava.redirect-uri:}") private val redirectUri: String,
    private val strava: StravaService,
    private val users: AppUserRepository,
    private val jwtService: JwtService,
    private val links: StravaConnectionRepository
) {

    // CHRÁNĚNO (vyžaduje Bearer) – vrací autorizační URL se state = náš app JWT
    @GetMapping("/oauth/url")
    fun oauthUrl(req: HttpServletRequest): Map<String, String> {
        if (clientId.isBlank() || redirectUri.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED,
                "Configure strava.client-id and strava.redirect-uri"
            )
        }
        val header = req.getHeader("Authorization") ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization")
        val token = header.removePrefix("Bearer ").trim().ifBlank { throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization") }

        val url = UriComponentsBuilder.fromUriString("https://www.strava.com/oauth/authorize")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("approval_prompt", "auto")
            .queryParam("scope", "read,activity:read_all")
            .queryParam("state", token)
            .build()
            .toUriString()
        return mapOf("url" to url)
    }

    // VEŘEJNÝ – Strava sem vrátí code + state (náš app JWT), tady uložíme tokeny k uživateli
    @GetMapping("/oauth/callback")
    fun oauthCallback(
        @RequestParam code: String,
        @RequestParam state: String
    ): ResponseEntity<Map<String, Any?>> {
        val username = runCatching { jwtService.extractUsername(state) }.getOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid state")
        val user = users.findByEmail(username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val tokens = strava.exchangeCode(code)
        val athleteId = (tokens.athlete?.get("id") as? Number)?.toLong()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Missing athlete id from Strava")

        val link = links.findByUserId(user.id!!).orElse(
            StravaConnection(
                userId = user.id!!,
                athleteId = athleteId,
                accessToken = tokens.access_token,
                refreshToken = tokens.refresh_token,
                expiresAt = tokens.expires_at
            )
        ).apply {
            this.accessToken = tokens.access_token
            this.refreshToken = tokens.refresh_token
            this.expiresAt = tokens.expires_at
        }
        links.save(link)

        // vrátíme jen neškodná data
        val body = mapOf(
            "linked" to true,
            "athleteId" to athleteId,
            "expiresAt" to tokens.expires_at
        )
        return ResponseEntity.ok(body)
    }
}
