package com.bikecare.bikecomponent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BikeComponentRepository : JpaRepository<BikeComponent, Long> {

    // --- Původní jednoduché signatury (ponecháme) ---
    fun findAllByBikeIdAndRemovedAtIsNullOrderByInstalledAtDesc(bikeId: Long): List<BikeComponent>
    fun findAllByBikeIdOrderByInstalledAtDesc(bikeId: Long): List<BikeComponent>

    // --- Overloady, které volá controller se současnou kontrolou vlastníka ---

    // list aktivních pro dané kolo + owner
    @Query("""
        select c
        from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
          and c.removedAt is null
        order by c.installedAt desc
    """)
    fun findAllActiveByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    // list všech pro dané kolo + owner
    @Query("""
        select c
        from BikeComponent c
        where c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
        order by c.installedAt desc
    """)
    fun findAllByBikeId(
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): List<BikeComponent>

    // detail komponenty (ne Optional) pro id + bikeId + owner
    @Query("""
        select c
        from BikeComponent c
        where c.id = :id
          and c.bike.id = :bikeId
          and c.bike.owner.id = :ownerId
    """)
    fun findByIdAndBikeId(
        @Param("id") id: Long,
        @Param("bikeId") bikeId: Long,
        @Param("ownerId") ownerId: Long
    ): BikeComponent
}
