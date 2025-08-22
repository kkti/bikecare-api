package com.bikecare.odometer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface OdometerEntryRepository : JpaRepository<OdometerEntry, Long> {

    @Query(
        """
        select o from OdometerEntry o
        where o.bike.id = :bikeId and o.bike.owner.id = :ownerId
        order by o.atDate asc
        """
    )
    fun findAllOwned(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<OdometerEntry>

    fun findTopByBike_IdOrderByAtDateDesc(bikeId: Long): OdometerEntry?

    fun findByBike_IdAndAtDate(bikeId: Long, atDate: LocalDate): OdometerEntry?

    fun deleteByBike_IdAndAtDate(bikeId: Long, atDate: LocalDate): Long
}
