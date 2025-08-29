package com.bikecare.bikecomponent.history

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ComponentEventRepository : JpaRepository<ComponentEvent, Long> {

    @Transactional
    fun deleteByComponentId(componentId: Long): Long

    @Query(
        """
        select e
        from ComponentEvent e
        where e.bikeId = :bikeId
          and e.componentId = :componentId
          and e.userId = :ownerId
        order by e.atTime asc
        """
    )
    fun findHistoryOwned(
        @Param("bikeId") bikeId: Long,
        @Param("componentId") componentId: Long,
        @Param("ownerId") ownerId: Long
    ): List<ComponentEvent>
}
