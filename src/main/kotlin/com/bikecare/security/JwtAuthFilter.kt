package com.bikecare.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    // Cesty, pro které se JWT vůbec nevyhodnocuje (veřejné / helpery)
    private val publicPrefixes = listOf(
        "/api/strava/oauth/callback",
        "/api/auth/",
        "/v3/api-docs",
        "/swagger-ui",
        "/swagger-ui.html",
        "/actuator/health"
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI ?: ""
        if (request.method.equals("OPTIONS", ignoreCase = true)) return true
        return publicPrefixes.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header.isNullOrBlank() || !header.startsWith("Bearer ", ignoreCase = true)) {
            chain.doFilter(request, response); return
        }

        val token = header.substringAfter(' ').trim()
        val username = runCatching { jwtService.extractUsername(token) }.getOrNull()

        if (!username.isNullOrBlank() && SecurityContextHolder.getContext().authentication == null) {
            val userDetails = runCatching { userDetailsService.loadUserByUsername(username) }.getOrNull()
            val valid = userDetails != null && runCatching { jwtService.isTokenValid(token, userDetails!!) }.getOrDefault(false)
            if (valid) {
                val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails!!.authorities).also {
                    it.details = WebAuthenticationDetailsSource().buildDetails(request)
                }
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        chain.doFilter(request, response)
    }
}
