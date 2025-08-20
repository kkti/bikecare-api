package com.bikecare.bikecomponent

import com.bikecare.bike.Bike
import com.bikecare.componenttype.ComponentType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity @Table(name = "bike_component")
class BikeComponent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bike_id", nullable = false) var bike: Bike,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "type_id", nullable = false) var type: ComponentType,
    var label: String? = null,
    @Column(columnDefinition = "component_pos") var position: String? = null,
    @Column(name="installed_at", nullable=false) var installedAt: Instant = Instant.now(),
    @Column(name="removed_at") var removedAt: Instant? = null,
    @Column(name="installed_odometer_km", precision=10, scale=1) var installedOdometerKm: BigDecimal? = null,
    @Column(name="lifespan_override", precision=10, scale=1) var lifespanOverride: BigDecimal? = null,
    @Column(precision=10, scale=2) var price: BigDecimal? = null,
    var currency: String? = null, var shop: String? = null,
    @Column(name="receipt_photo_url") var receiptPhotoUrl: String? = null
)
