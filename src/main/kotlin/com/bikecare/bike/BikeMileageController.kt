package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

data class OdometerDto(val bikeId: Long, val odometerMeters: Long)
data class UpdateOdometerRequest(val op: String, @field:Min(0) val meters: Long)

@RestController
@RequestMapping("/api/bikes/{bikeId}/mileage")
class BikeMileageController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository,
    private val statsRepo: BikeStatsRepository
) {
    @GetMapping
    fun get(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long
    ): OdometerDto {
        val user = users.findByEmail(principal.username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val bike = bikes.findByIdAndOwnerId(bikeId, user.id!!).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }
        val stats = statsRepo.findByBikeId(bike.id!!).orElseGet { statsRepo.save(BikeStats(bikeId = bike.id!!)) }
        return OdometerDto(bikeId = bike.id!!, odometerMeters = stats.odometerMeters)
    }

    @PatchMapping
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @RequestBody body: UpdateOdometerRequest
    ): OdometerDto {
        val user = users.findByEmail(principal.username) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val bike = bikes.findByIdAndOwnerId(bikeId, user.id!!).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }
        val stats = statsRepo.findByBikeId(bike.id!!).orElseGet { statsRepo.save(BikeStats(bikeId = bike.id!!)) }

        when (body.op.lowercase()) {
            "set" -> {
                if (body.meters < stats.odometerMeters)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "New odometer must be >= current value")
                stats.odometerMeters = body.meters
            }
            "add" -> stats.odometerMeters = stats.odometerMeters + body.meters
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported op '${body.op}'")
        }
        val saved = statsRepo.save(stats)
        return OdometerDto(bikeId = bike.id!!, odometerMeters = saved.odometerMeters)
    }
}
