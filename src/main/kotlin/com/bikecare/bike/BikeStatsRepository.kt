package com.bikecare.bike

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BikeStatsRepository : JpaRepository<BikeStats, Long> {
    fun findByBikeId(bikeId: Long): Optional<BikeStats>
}
