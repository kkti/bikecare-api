package com.bikecare.servicelog

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ServiceRecordRepository : JpaRepository<ServiceRecord, Long> {
    fun findAllByBikeIdOrderByPerformedAtDesc(bikeId: Long): List<ServiceRecord>
    fun findByIdAndBikeId(id: Long, bikeId: Long): Optional<ServiceRecord>
}
