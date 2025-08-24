package com.bikecare.odometer

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface OdometerRepository : JpaRepository<Odometer, Long> {
    fun findTopByBikeIdOrderByAtDateDesc(bikeId: Long): Odometer?
    fun findTopByBikeIdAndAtDateLessThanEqualOrderByAtDateDesc(bikeId: Long, atDate: LocalDate): Odometer?
    fun findByBikeIdAndAtDate(bikeId: Long, atDate: LocalDate): Odometer?
    fun findAllByBikeIdOrderByAtDateAsc(bikeId: Long): List<Odometer>
}
