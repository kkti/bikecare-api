package com.bikecare.bikecomponent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {

    @Query(
        """
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
          and c.removedAt is null
        order by c.installedAt desc, c.id desc
        """
    )
    fun findAllActiveByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    @Query(
        """
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
        order by c.installedAt desc, c.id desc
        """
    )
    fun findAllByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    @Query(
        """
        select c from BikeComponent c
        where c.id = :componentId
          and c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
        """
    )
    fun findByIdAndBikeId(
        @Param("componentId") componentId: Long,
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): BikeComponent?
}