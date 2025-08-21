package com.bikecare.odometer

import com.bikecare.bike.Bike
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "odometer_entry",
    uniqueConstraints = [UniqueConstraint(name = "uq_odometer_bike_date", columnNames = ["bike_id", "at_date"])]
)
class OdometerEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_id", nullable = false)
    var bike: Bike,

    @Column(name = "at_date", nullable = false)
    var atDate: LocalDate,

    @Column(name = "km", nullable = false, precision = 10, scale = 1)
    var km: BigDecimal,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
