package com.bikecare.bike

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "service_record")
data class ServiceRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bike_id", nullable = false)
    val bikeId: Long,

    @Column(name = "performed_at", nullable = false)
    var performedAt: Instant = Instant.now(),

    @Column(name = "odometer_meters", nullable = false)
    var odometerMeters: Long,

    var note: String? = null
)
