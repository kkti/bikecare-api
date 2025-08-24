package com.bikecare.servicelog

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "service_record")
data class ServiceRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "bike_id", nullable = false)
    val bikeId: Long,

    @Column(name = "component_id")
    val componentId: Long? = null,

    @Column(name = "performed_at", nullable = false)
    val performedAt: Instant = Instant.now(),

    @Column(name = "description", nullable = false)
    val description: String,

    @Column(name = "km_at_service")
    val kmAtService: BigDecimal? = null,

    @Column(name = "cost")
    val cost: BigDecimal? = null,

    @Column(name = "currency", nullable = false)
    val currency: String = "CZK",

    @Column(name = "note")
    val note: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
