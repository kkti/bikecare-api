package com.bikecare.user

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserService(
    private val userRepository: AppUserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val u = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        // Pokud AppUser implementuje UserDetails, klidně vrať přímo `u`
        // return u

        // Univerzální varianta (funguje i když AppUser UserDetails NEimplementuje)
        return User.withUsername(u.email)
            .password(u.password)
            .roles("USER") // jednoduché default role
            .build()
    }
}
