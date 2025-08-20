// src/main/kotlin/com/bikecare/componenttype/ComponentType.kt
package com.bikecare.componenttype

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "component_type")
class ComponentType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var key: String,

    @Column(nullable = false)
    var name: String,

    @Column(name = "default_lifespan")
    var defaultLifespan: BigDecimal? = null,

    @Column(name = "default_service_interval")
    var defaultServiceInterval: BigDecimal? = null,

    @Column(nullable = false)
    var unit: String
)
