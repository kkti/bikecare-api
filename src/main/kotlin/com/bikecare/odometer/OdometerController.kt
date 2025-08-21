package com.bikecare.odometer

import com.bikecare.bike.BikeRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

data class OdometerUpsertRequest(
    @field:NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val date: LocalDate,

    @field:NotNull @field:PositiveOrZero
    val km: BigDecimal
)

@Tag(name = "Odometer")
@RestController
@RequestMapping("/api/bikes/{bikeId}/odometer")
class OdometerController(
    private val bikeRepo: BikeRepository,
    private val odoRepo: OdometerEntryRepository,
    private val users: AppUserRepository
) {

    private fun requireOwnedBike(bikeId: Long, ownerId: Long) =
        bikeRepo.findByIdAndOwnerId(bikeId, ownerId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

    @GetMapping
    @Operation(summary = "Seznam záznamů nájezdu pro kolo (nejnovější první)")
    fun list(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<OdometerEntry> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        requireOwnedBike(bikeId, user.id!!)
        return if (from != null && to != null) {
            odoRepo.findRangeOwned(bikeId, user.id!!, from, to)
        } else {
            odoRepo.findAllOwned(bikeId, user.id!!)
        }
    }

    @GetMapping("/current")
    @Operation(summary = "Aktuální nájezd podle posledního záznamu")
    fun current(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<Map<String, Any?>> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        requireOwnedBike(bikeId, user.id!!)
        val latest = odoRepo.findTop1ByBikeOwnedOrderByAtDateDesc(bikeId, user.id!!).firstOrNull()
        return ResponseEntity.ok(
            mapOf(
                "bikeId" to bikeId,
                "date" to latest?.atDate,
                "km" to latest?.km
            )
        )
    }

    @PostMapping
    @Transactional
    @Operation(
        summary = "Vytvoří/aktualizuje záznam nájezdu (upsert dle data)",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [Content(schema = Schema(implementation = OdometerEntry::class))]
            )
        ]
    )
    fun upsert(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody req: OdometerUpsertRequest
    ): OdometerEntry {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val bike = requireOwnedBike(bikeId, user.id!!)
        val existing = odoRepo.findAllOwned(bikeId, user.id!!).firstOrNull { it.atDate == req.date }
        return if (existing != null) {
            existing.km = req.km
            odoRepo.save(existing)
        } else {
            odoRepo.save(OdometerEntry(bike = bike, atDate = req.date, km = req.km))
        }
    }

    @DeleteMapping
    @Transactional
    @Operation(summary = "Smaže záznam za daný den (query param date=YYYY-MM-DD)")
    fun deleteByDate(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        requireOwnedBike(bikeId, user.id!!)
        val list = odoRepo.findAllOwned(bikeId, user.id!!).filter { it.atDate == date }
        list.forEach { odoRepo.delete(it) }
        return ResponseEntity.noContent().build()
    }
}
