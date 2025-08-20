package com.bikecare.security

import com.bikecare.user.AppUserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwt: JwtService,
    private val users: AppUserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header?.startsWith("Bearer ") == true && SecurityContextHolder.getContext().authentication == null) {
            val token = header.substring(7)
            runCatching {
                val email = jwt.extractEmail(token)
                val user = users.findByEmail(email).orElse(null)
                if (user != null) {
                    val details: UserDetails = User.withUsername(user.email)
                        .password(user.password)
                        .authorities("ROLE_${user.role}")
                        .build()
                    val auth = UsernamePasswordAuthenticationToken(details, null, details.authorities)
                    auth.details = WebAuthenticationDetailsSource().buildDetails(req)
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
        chain.doFilter(req, res)
    }
}
