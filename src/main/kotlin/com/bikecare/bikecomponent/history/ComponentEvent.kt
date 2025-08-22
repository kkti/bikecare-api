package com.bikecare.bikecomponent.history

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "component_event") // <<< MUSÍ sedět na název v DB
class ComponentEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "component_id", nullable = false)
    var componentId: Long,

    @Column(name = "bike_id", nullable = false)
    var bikeId: Long,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: EventType,

    @Column(name = "at_time", nullable = false)
    var atTime: Instant,

    @Column(name = "odometer_km")
    var odometerKm: BigDecimal? = null
)
