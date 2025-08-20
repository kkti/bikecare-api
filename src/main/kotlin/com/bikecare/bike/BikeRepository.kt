package com.bikecare.bike

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BikeRepository : JpaRepository<Bike, Long> {
    fun findAllByOwner_Id(ownerId: Long): List<Bike>
    fun findByIdAndOwner_Id(id: Long, ownerId: Long): Optional<Bike>
}
