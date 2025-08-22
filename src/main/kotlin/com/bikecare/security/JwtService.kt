package com.bikecare.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.Date

@Service
class JwtService(
    @Value("\${security.jwt.secret}") private val secret: String,
    @Value("\${security.jwt.expiration}") private val expirationMs: Long
) {
    /** Vygeneruje JWT pro dané uživatelské jméno (email). */
    fun generateToken(username: String): String = buildToken(username)

    /** Overload – když bys někde měl rovnou UserDetails. */
    fun generateToken(userDetails: UserDetails): String = buildToken(userDetails.username)

    /** Z tokenu vytáhne subject (uživatelské jméno / email). */
    fun extractUsername(token: String): String =
        extractAllClaims(token).subject

    /** Ověří platnost proti UserDetails – používá JwtAuthFilter. */
    fun isTokenValid(token: String, userDetails: UserDetails): Boolean =
        extractUsername(token) == userDetails.username && !isExpired(token)

    // ===== interní pomocné =====
    private fun buildToken(subject: String): String {
        val now = Date()
        val exp = Date(now.time + expirationMs)
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(signingKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun extractAllClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .body

    private fun isExpired(token: String): Boolean =
        extractAllClaims(token).expiration.before(Date())

    private fun signingKey(): Key =
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
}
