package com.bikecare.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.ttlMinutes:1440}") private val ttlMinutes: Long
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generate(email: String): String {
        val now = Instant.now()
        val exp = now.plusSeconds(ttlMinutes * 60)
        return Jwts.builder()
            .subject(email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key)
            .compact()
    }

    fun extractEmail(token: String): String =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
}
