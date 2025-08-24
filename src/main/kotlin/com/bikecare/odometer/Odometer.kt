package com.bikecare.odometer

import com.bikecare.bike.Bike
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "odometer")
class Odometer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    var bike: Bike,

    @Column(name = "at_date", nullable = false)
    var atDate: LocalDate,

    @Column(name = "km", nullable = false, precision = 12, scale = 2)
    var km: BigDecimal
)
