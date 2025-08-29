package com.bikecare.bikecomponent

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {

    @Query("""
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
          and c.removedAt is null
        order by c.installedAt desc
    """)
    fun findAllActiveByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    @Query("""
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
        order by c.installedAt desc
    """)
    fun findAllByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    @Query("""
        select c from BikeComponent c
        where c.id = :componentId
          and c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
    """)
    fun findByIdAndBikeId(
        @Param("componentId") componentId: Long,
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): BikeComponent?

    @Query("""
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
    """)
    fun findPageByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long,
        pageable: Pageable
    ): Page<BikeComponent>

    @Query("""
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
          and c.removedAt is null
    """)
    fun findPageActiveByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long,
        pageable: Pageable
    ): Page<BikeComponent>

    // --- FIX: explicitní cast paramů na string kvůli Postgresu ---
    @Query("""
        select c from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
          and (:activeOnly = false or c.removedAt is null)
          and (:typeKey is null or lower(c.typeKey) = lower(cast(:typeKey as string)))
          and (:position is null or c.position = :position)
          and (
              :labelLike is null
              or lower(c.label) like concat('%', lower(cast(:labelLike as string)), '%')
          )
    """)
    fun findPageByFilters(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long,
        @Param("activeOnly") activeOnly: Boolean,
        @Param("typeKey") typeKey: String?,
        @Param("position") position: Position?,
        @Param("labelLike") labelLike: String?,
        pageable: Pageable
    ): Page<BikeComponent>
}
