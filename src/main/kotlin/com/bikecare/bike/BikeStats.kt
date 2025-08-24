package com.bikecare.bike

import jakarta.persistence.*

@Entity
@Table(name = "bike_stats")
data class BikeStats(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bike_id", nullable = false, unique = true)
    val bikeId: Long,

    @Column(name = "odometer_meters", nullable = false)
    var odometerMeters: Long = 0
)
