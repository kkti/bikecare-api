package com.bikecare.componenttype

import jakarta.persistence.*
import java.math.BigDecimal

@Entity @Table(name = "component_type")
class ComponentType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    val key: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val unit: String, // "KM","HOURS","DAYS","ELEVATION_M"

    @Column(name = "default_lifespan", precision = 10, scale = 1)
    val defaultLifespan: BigDecimal? = null,

    @Column(name = "default_service_interval", precision = 10, scale = 1)
    val defaultServiceInterval: BigDecimal? = null
)
