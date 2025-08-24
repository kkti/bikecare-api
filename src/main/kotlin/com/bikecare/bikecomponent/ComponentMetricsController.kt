package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.odometer.OdometerRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

data class ComponentMetricsDto(
    val id: Long,
    val typeKey: String,
    val typeName: String?,
    val label: String?,
    val position: String?,
    val installedAt: Instant?,
    val removedAt: Instant?,
    val installedOdometerKm: BigDecimal?,
    val endOdometerKm: BigDecimal?,     // aktuální km nebo km k datu odebrání
    val kmSinceInstall: BigDecimal?,    // end - installed (>= 0)
    val lifetimeDays: Long?
)

@Tag(name = "Component metrics")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class ComponentMetricsController(
    private val users: AppUserRepository,
    private val bikes: BikeRepository,
    private val components: BikeComponentRepository,
    private val odometers: OdometerRepository
) {

    @GetMapping("/metrics")
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @RequestParam(defaultValue = "true") activeOnly: Boolean
    ): List<ComponentMetricsDto> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val bike = bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        // ⬇️ doplněn ownerId
        val comps = if (activeOnly) {
            components.findAllActiveByBikeId(bike.id!!, user.id!!)
        } else {
            components.findAllByBikeId(bike.id!!, user.id!!)
        }

        val currentKm = odometers.findTopByBikeIdOrderByAtDateDesc(bike.id!!)?.km

        return comps.map { c ->
            val endKm: BigDecimal? = if (c.removedAt != null) {
                val endDate = c.removedAt!!.atZone(ZoneOffset.UTC).toLocalDate()
                odometers.findTopByBikeIdAndAtDateLessThanEqualOrderByAtDateDesc(bike.id!!, endDate)?.km
            } else {
                currentKm
            }

            val kmSince: BigDecimal? = if (c.installedOdometerKm != null && endKm != null) {
                (endKm.subtract(c.installedOdometerKm)).max(BigDecimal.ZERO)
            } else null

            val lifetime = c.installedAt?.let { start ->
                val to = c.removedAt ?: Instant.now()
                Duration.between(start, to).toDays()
            }

            ComponentMetricsDto(
                id = c.id!!,
                typeKey = c.typeKey,
                typeName = c.typeName,
                label = c.label,
                position = c.position?.name,
                installedAt = c.installedAt,
                removedAt = c.removedAt,
                installedOdometerKm = c.installedOdometerKm,
                endOdometerKm = endKm,
                kmSinceInstall = kmSince,
                lifetimeDays = lifetime
            )
        }
    }
}
