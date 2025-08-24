package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

data class ServiceRecordDto(
    val id: Long?,
    val performedAt: Instant,
    val odometerMeters: Long,
    val note: String?
)

data class CreateServiceRecordRequest(
    val performedAt: Instant? = null,
    @field:Min(0) val odometerMeters: Long,
    val note: String? = null
)

@RestController
@RequestMapping("/api/bikes/{bikeId}/service-records")
class ServiceRecordController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository,
    private val statsRepo: BikeStatsRepository,
    private val records: ServiceRecordRepository
) {
    @GetMapping
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long
    ): List<ServiceRecordDto> {
        ownerCheck(principal, bikeId)
        return records.findAllByBikeIdOrderByPerformedAtDesc(bikeId).map { it.toDto() }
    }

    @PostMapping
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @Valid @RequestBody body: CreateServiceRecordRequest
    ): ServiceRecordDto {
        ownerCheck(principal, bikeId)
        val whenAt = body.performedAt ?: Instant.now()
        val saved = records.save(
            ServiceRecord(
                bikeId = bikeId,
                performedAt = whenAt,
                odometerMeters = body.odometerMeters,
                note = body.note
            )
        )
        // Volitelne posunout i odometr
        val stats = statsRepo.findByBikeId(bikeId).orElseGet { statsRepo.save(BikeStats(bikeId = bikeId)) }
        if (body.odometerMeters > stats.odometerMeters) {
            stats.odometerMeters = body.odometerMeters
            statsRepo.save(stats)
        }
        return saved.toDto()
    }

    private fun ownerCheck(principal: UserDetails, bikeId: Long) {
        val user = users.findByEmail(principal.username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        bikes.findByIdAndOwnerId(bikeId, user.id!!).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }
    }
}

private fun ServiceRecord.toDto() = ServiceRecordDto(
    id = id,
    performedAt = performedAt,
    odometerMeters = odometerMeters,
    note = note
)
