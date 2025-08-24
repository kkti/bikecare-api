package com.bikecare.strava

import com.bikecare.user.AppUserRepository
import com.bikecare.security.JwtService
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

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
    // CHRÁNĚNO – vrací autorizační URL se state = náš app JWT
    @GetMapping("/oauth/url")
    fun oauthUrl(@Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails): Map<String, String> {
        if (clientId.isBlank() || redirectUri.isBlank()) {
            throw ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Configure strava.client-id and strava.redirect-uri")
        }
        val token = jwtService.generateToken(principal.username) // pokud generátor existuje; alternativně přijmi header a přepošli
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

    // VEŘEJNÉ – callback z OAuth. Uloží/aktualizuje tokeny k uživateli podle state (náš app JWT).
    @GetMapping("/oauth/callback")
    fun oauthCallback(@RequestParam code: String, @RequestParam state: String): ResponseEntity<Map<String, Any?>> {
        val username = runCatching { jwtService.extractUsername(state) }.getOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid state")
        val user = users.findByEmail(username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val tokens = strava.exchangeCode(code)
        val athleteId = (tokens.athlete?.get("id") as? Number)?.toLong()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Missing athlete id from Strava")

        val link = links.findByUserId(user.id!!).orElse(
            StravaConnection(userId = user.id!!, athleteId = athleteId,
                accessToken = tokens.access_token, refreshToken = tokens.refresh_token, expiresAt = tokens.expires_at)
        ).apply {
            this.accessToken = tokens.access_token
            this.refreshToken = tokens.refresh_token
            this.expiresAt = tokens.expires_at
        }
        links.save(link)
        return ResponseEntity.ok(mapOf("linked" to true, "athleteId" to athleteId, "expiresAt" to tokens.expires_at))
    }

    // CHRÁNĚNO – vrátí /api/v3/athlete, s auto-refresh 10 min před expirací
    @GetMapping("/athlete")
    fun athlete(@Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails): ResponseEntity<Map<String, Any>> {
        val user = users.findByEmail(principal.username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val link = links.findByUserId(user.id!!).orElseThrow { ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Strava not linked") }

        val now = System.currentTimeMillis() / 1000
        val margin = 600 // 10 minut
        if (link.expiresAt <= now + margin) {
            val t = strava.refreshToken(link.refreshToken)
            link.accessToken = t.access_token
            link.refreshToken = t.refresh_token
            link.expiresAt = t.expires_at
            links.save(link)
        }

        val data = strava.getAthlete(link.accessToken)
        return ResponseEntity.ok(data)
    }

    // CHRÁNĚNO – odpojení (volitelně i na Stravě)
    @DeleteMapping("/connection")
    fun deauthorize(@Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val link = links.findByUserId(user.id!!).orElseThrow { ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Strava not linked") }
        runCatching { strava.deauthorize(link.accessToken) } // best effort
        links.delete(link)
        return ResponseEntity.noContent().build()
    }
}
