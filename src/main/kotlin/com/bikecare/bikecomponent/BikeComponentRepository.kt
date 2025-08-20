package com.bikecare.bikecomponent
import org.springframework.data.jpa.repository.JpaRepository

interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {
    fun findAllByBikeIdAndRemovedAtIsNull(bikeId: Long): List<BikeComponent>
}
