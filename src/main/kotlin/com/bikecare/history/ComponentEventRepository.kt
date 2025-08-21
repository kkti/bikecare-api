package com.bikecare.bikecomponent.history

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ComponentEventRepository : JpaRepository<ComponentEvent, Long> {

    @Query(
        """
        select e from ComponentEvent e
        where e.component.id = :componentId
          and e.bike.id = :bikeId
          and e.bike.owner.id = :ownerId
        order by e.atTime desc
        """
    )
    fun findHistoryOwned(
        @Param("bikeId") bikeId: Long,
        @Param("componentId") componentId: Long,
        @Param("ownerId") ownerId: Long
    ): List<ComponentEvent>
}
