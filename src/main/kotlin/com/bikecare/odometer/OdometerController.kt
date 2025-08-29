package com.bikecare.odometer

import com.bikecare.bike.BikeRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

@Tag(name = "Odometer")
@RestController
@RequestMapping("/api/bikes/{bikeId}/odometer")
class OdometerController(
    private val users: AppUserRepository,
    private val bikes: BikeRepository,
    private val odometers: OdometerRepository
) {
    data class OdometerRequest(
        val atDate: LocalDate,
        @field:PositiveOrZero val km: BigDecimal
    )
    data class OdometerResponse(
        val id: Long,
        val atDate: LocalDate,
        val km: BigDecimal
    )

    private fun ownerId(principal: UserDetails): Long =
        users.findByEmail(principal.username)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

    private fun requireBike(bikeId: Long, ownerId: Long) =
        bikes.findByIdAndOwnerId(bikeId, ownerId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
        }

    @Operation(summary = "Create or update odometer reading for a specific date")
    @PostMapping
    fun upsert(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: OdometerRequest
    ): ResponseEntity<OdometerResponse> {
        val uid = ownerId(principal)
        val bike = requireBike(bikeId, uid)

        val existing = odometers.findByBikeIdAndAtDate(bike.id!!, body.atDate)
        val entity = (existing ?: Odometer(
            bike = bike,
            atDate = body.atDate,
            km = body.km
        )).apply {
            this.km = body.km
        }

        val saved = odometers.save(entity)
        val dto = OdometerResponse(saved.id!!, saved.atDate, saved.km)
        return if (existing == null) ResponseEntity.status(HttpStatus.CREATED).body(dto)
               else ResponseEntity.ok(dto)
    }

    @Operation(summary = "Get latest odometer reading")
    @GetMapping("/latest")
    fun latest(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<OdometerResponse> {
        val uid = ownerId(principal)
        val bike = requireBike(bikeId, uid)
        val last = odometers.findTopByBikeIdOrderByAtDateDesc(bike.id!!)
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(OdometerResponse(last.id!!, last.atDate, last.km))
    }
}
