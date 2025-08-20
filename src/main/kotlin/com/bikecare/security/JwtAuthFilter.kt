package com.bikecare.security

import com.bikecare.user.AppUserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val users: AppUserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substringAfter("Bearer ").trim()
        if (token.isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        val username = try { jwtService.extractUsername(token) } catch (_: Exception) { null }

        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            val user = users.findByEmail(username)
            if (user != null && jwtService.isTokenValid(token, user.email)) {
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${(user.role ?: "USER").uppercase()}"))
                // nastavíme principal jako UserDetails (ne String), aby fungovalo @AuthenticationPrincipal UserDetails
                val principal = User(user.email, user.password, authorities)
                val authToken = UsernamePasswordAuthenticationToken(principal, null, authorities)
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
