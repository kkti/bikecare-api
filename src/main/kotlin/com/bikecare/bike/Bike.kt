package com.bikecare.bike

import com.bikecare.user.AppUser
import jakarta.persistence.*
import java.time.Instant

@Entity @Table(name = "bike")
class Bike(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: AppUser,

    @Column(nullable = false) var name: String,
    var brand: String? = null,
    var model: String? = null,
    var type: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
