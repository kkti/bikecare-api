package com.bikecare.bike

import org.springframework.data.jpa.repository.JpaRepository

interface ServiceRecordRepository : JpaRepository<ServiceRecord, Long> {
    fun findAllByBikeIdOrderByPerformedAtDesc(bikeId: Long): List<ServiceRecord>
}
