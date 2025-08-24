package com.bikecare.strava

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StravaConnectionRepository : JpaRepository<StravaConnection, Long> {
    fun findByUserId(userId: Long): Optional<StravaConnection>
}
