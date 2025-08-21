package com.bikecare.odometer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.*

interface OdometerEntryRepository : JpaRepository<OdometerEntry, Long> {

    @Query(
        """
        select o from OdometerEntry o
        where o.bike.id = :bikeId and o.bike.owner.id = :ownerId
        order by o.atDate desc
        """
    )
    fun findAllOwned(@Param("bikeId") bikeId: Long, @Param("ownerId") ownerId: Long): List<OdometerEntry>

    @Query(
        """
        select o from OdometerEntry o
        where o.bike.id = :bikeId and o.bike.owner.id = :ownerId and o.atDate between :from and :to
        order by o.atDate desc
        """
    )
    fun findRangeOwned(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate
    ): List<OdometerEntry>

    @Query(
        """
        select o from OdometerEntry o
        where o.bike.id = :bikeId and o.bike.owner.id = :ownerId
        order by o.atDate desc
        """
    )
    fun findTop1ByBikeOwnedOrderByAtDateDesc(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<OdometerEntry> // použijeme .firstOrNull()
}
