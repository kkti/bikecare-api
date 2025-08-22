package com.bikecare.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import com.bikecare.security.JwtService
import com.bikecare.user.AppUser
import com.bikecare.user.AppUserRepository

data class RegisterRequest(
    @field:Email val email: String,
    @field:NotBlank val password: String
)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val repo: AppUserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService,
    private val authManager: AuthenticationManager
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: RegisterRequest): ResponseEntity<AuthResponse> {
        if (repo.existsByEmail(body.email)) return ResponseEntity.badRequest().build()
        val saved = repo.save(AppUser(email = body.email, password = encoder.encode(body.password)))
        return ResponseEntity.ok(AuthResponse(token = jwt.generateToken(saved.email)))
    }

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest): ResponseEntity<AuthResponse> {
        authManager.authenticate(UsernamePasswordAuthenticationToken(body.email, body.password))
        return ResponseEntity.ok(AuthResponse(token = jwt.generateToken(body.email)))
    }
}
