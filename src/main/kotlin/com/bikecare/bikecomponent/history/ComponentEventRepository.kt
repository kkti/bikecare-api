package com.bikecare.bikecomponent.history

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ComponentEventRepository : JpaRepository<ComponentEvent, Long> {

    fun deleteByComponentIdAndUserId(componentId: Long, userId: Long): Long

    @Query(
        """
        select e
        from ComponentEvent e
        where e.componentId = :componentId
          and e.bikeId = :bikeId
          and e.userId = :userId
        order by e.atTime desc
        """
    )
    fun findHistoryOwned(
        @Param("componentId") componentId: Long,
        @Param("bikeId") bikeId: Long,
        @Param("userId") userId: Long
    ): List<ComponentEvent>
}
