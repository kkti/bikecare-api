package com.bikecare.bike

import com.bikecare.user.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BikeRepository : JpaRepository<Bike, Long> {
    fun findAllByOwner(owner: AppUser): List<Bike>
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Optional<Bike>
}
