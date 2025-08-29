package com.bikecare.odometer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OdometerRepository : JpaRepository<Odometer, Long> {
    fun findTopByBikeIdOrderByAtDateDesc(bikeId: Long): Odometer?
    fun findTopByBikeIdAndAtDateLessThanEqualOrderByAtDateDesc(bikeId: Long, atDate: LocalDate): Odometer?
    fun findByBikeIdAndAtDate(bikeId: Long, atDate: LocalDate): Odometer?
}
