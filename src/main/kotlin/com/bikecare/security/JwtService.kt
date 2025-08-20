package com.bikecare.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.issuer}") private val issuer: String,
    @Value("\${jwt.ttlMinutes}") private val ttlMinutes: Long
) {
    private val key: Key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generate(username: String): String {
        val now = Instant.now()
        val expiry = now.plus(ttlMinutes, ChronoUnit.MINUTES)
        return Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUsername(token: String): String =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject

    fun isTokenValid(token: String, username: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            val notExpired = claims.expiration.after(Date())
            val subjectMatches = claims.subject == username
            notExpired && subjectMatches
        } catch (_: Exception) {
            false
        }
    }
}
