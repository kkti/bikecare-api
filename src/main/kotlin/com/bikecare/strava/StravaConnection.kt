package com.bikecare.strava

import jakarta.persistence.*

@Entity
@Table(name = "strava_connection")
data class StravaConnection(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,
    @Column(name = "athlete_id", nullable = false)
    val athleteId: Long,
    @Column(name = "access_token", nullable = false)
    var accessToken: String,
    @Column(name = "refresh_token", nullable = false)
    var refreshToken: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Long
)
