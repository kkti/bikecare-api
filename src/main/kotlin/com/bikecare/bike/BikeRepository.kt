package com.bikecare.bike

import org.springframework.data.jpa.repository.JpaRepository

interface BikeRepository : JpaRepository<Bike, Long> {
    fun findAllByOwnerId(ownerId: Long): List<Bike>
}
