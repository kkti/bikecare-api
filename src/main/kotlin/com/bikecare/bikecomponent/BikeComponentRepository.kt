package com.bikecare.bikecomponent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {
    fun findAllByBikeIdAndRemovedAtIsNull(bikeId: Long): List<BikeComponent>
    fun findAllByBikeId(bikeId: Long): List<BikeComponent>
    fun findByIdAndBikeId(id: Long, bikeId: Long): Optional<BikeComponent>
}