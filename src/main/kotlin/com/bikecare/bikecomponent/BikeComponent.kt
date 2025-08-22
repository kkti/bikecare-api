package com.bikecare.bikecomponent

import com.bikecare.bike.Bike
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "bike_components")
class BikeComponent(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    var bike: Bike,

    @Column(name = "type_key", nullable = false, length = 64)
    var typeKey: String,

    @Column(name = "type_name", nullable = false, length = 128)
    var typeName: String,

    @Column(name = "label")
    var label: String? = null,

    @Column(name = "position", length = 16)
    @Enumerated(EnumType.STRING)
    var position: Position = Position.REAR,

    @Column(name = "installed_at", nullable = false)
    var installedAt: Instant = Instant.now(),

    @Column(name = "installed_odometer_km")
    var installedOdometerKm: BigDecimal? = null,

    @Column(name = "lifespan_override")
    var lifespanOverride: BigDecimal? = null,

    @Column(name = "price")
    var price: BigDecimal? = null,

    @Column(name = "currency", length = 8)
    var currency: String? = null,

    @Column(name = "shop")
    var shop: String? = null,

    @Column(name = "receipt_photo_url")
    var receiptPhotoUrl: String? = null,

    @Column(name = "removed_at")
    var removedAt: Instant? = null,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

enum class Position { FRONT, REAR, LEFT, RIGHT, ANY }
