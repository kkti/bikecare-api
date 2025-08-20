package com.bikecare.user

import jakarta.persistence.*
import java.time.Instant

@Entity @Table(name = "app_user")
class AppUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    var role: String = "USER",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
