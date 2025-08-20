package com.bikecare.bikecomponent

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {
    fun findAllByBike_IdAndRemovedAtIsNull(bikeId: Long): List<BikeComponent>
    fun findAllByBike_Id(bikeId: Long): List<BikeComponent>
    fun findByIdAndBike_Id(id: Long, bikeId: Long): Optional<BikeComponent>
}
