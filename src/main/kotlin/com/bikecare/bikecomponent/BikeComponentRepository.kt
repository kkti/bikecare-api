package com.bikecare.bikecomponent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {

    fun findAllByBikeId(bikeId: Long): List<BikeComponent>

    @Query("select bc from BikeComponent bc where bc.bike.id = :bikeId and bc.removedAt is null")
    fun findAllActiveByBikeId(@Param("bikeId") bikeId: Long): List<BikeComponent>

    fun findByIdAndBikeId(id: Long, bikeId: Long): Optional<BikeComponent>
}
