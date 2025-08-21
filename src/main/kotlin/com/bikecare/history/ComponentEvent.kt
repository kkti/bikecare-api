package com.bikecare.bikecomponent.history

import com.bikecare.bike.Bike
import com.bikecare.bikecomponent.BikeComponent
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "component_event")
class ComponentEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    var bike: Bike,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    var component: BikeComponent,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: ComponentEventType,

    @Column(name = "at_time", nullable = false)
    var atTime: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "note")
    var note: String? = null,

    @Column(name = "odometer_km", precision = 10, scale = 1)
    var odometerKm: BigDecimal? = null
)
