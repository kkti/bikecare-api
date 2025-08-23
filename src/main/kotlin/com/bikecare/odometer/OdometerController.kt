package com.bikecare.odometer

import com.bikecare.bike.BikeRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.format.annotation.DateTimeFormat
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
    private val odoRepo: OdometerEntryRepository,
    private val bikes: BikeRepository,
    private val users: AppUserRepository
) {

    data class UpsertRequest(
        @field:NotNull
        @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val date: LocalDate?,
        @field:NotNull @field:PositiveOrZero
        val km: BigDecimal?
    )

    private fun requireOwnedBike(bikeId: Long, userId: Long) {
        val exists = bikes.findByIdAndOwnerId(bikeId, userId).isPresent
        if (!exists) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
    }

    @Operation(summary = "Create or update odometer entry for a given date")
    @PutMapping
    fun upsert(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: UpsertRequest
    ): ResponseEntity<OdometerEntry> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        requireOwnedBike(bikeId, user.id!!)

        val date = body.date!!
        val km = body.km!!

        val existing = odoRepo.findByBike_IdAndAtDate(bikeId, date)
        val entity = if (existing != null) {
            existing.km = km
            existing
        } else {
            OdometerEntry(
                bike = bikes.getReferenceById(bikeId),
                atDate = date,
                km = km
            )
        }
        val saved = odoRepo.save(entity)
        return ResponseEntity.ok(saved)
    }

    @Operation(summary = "Get the current (latest) odometer value")
    @GetMapping("/current")
    fun current(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<Map<String, Any>> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        requireOwnedBike(bikeId, user.id!!)
        val latest = odoRepo.findTopByBike_IdOrderByAtDateDesc(bikeId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No odometer entries")
        return ResponseEntity.ok(
            mapOf(
                "bikeId" to bikeId,
                "date" to latest.atDate,
                "km" to latest.km
            )
        )
    }

    @Operation(summary = "Delete entry for a given date")
    @DeleteMapping
    fun deleteByDate(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        requireOwnedBike(bikeId, user.id!!)
        odoRepo.deleteByBike_IdAndAtDate(bikeId, date)
        return ResponseEntity.noContent().build()
    }
}
