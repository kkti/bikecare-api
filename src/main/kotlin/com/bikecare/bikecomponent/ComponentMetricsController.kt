package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.user.AppUserRepository
import com.bikecare.componenttype.ComponentTypeRepository
import com.bikecare.odometer.OdometerEntryRepository
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class ComponentWithMetricsResponse(
    val id: Long,
    val typeKey: String?,
    val typeName: String?,
    val label: String?,
    val position: String?,
    val installedAt: Instant?,
    val installedOdometerKm: BigDecimal?,
    val lifespanOverride: BigDecimal?,
    val price: BigDecimal?,
    val currency: String?,
    val shop: String?,
    val receiptPhotoUrl: String?,
    val removedAt: Instant?,
    // metriky:
    val usedKm: BigDecimal?,
    val remainingKm: BigDecimal?,
    val percentUsed: BigDecimal?,
    val dueReplacement: Boolean?
)

@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class ComponentMetricsController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository,
    private val compTypes: ComponentTypeRepository,
    private val odoRepo: OdometerEntryRepository,
    private val reader: ComponentReader
) {
    @GetMapping("/metrics")
    fun listWithMetrics(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable @NotNull bikeId: Long,
        @RequestParam(defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<List<ComponentWithMetricsResponse>> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // owner check
        bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        // poslední odometr (km) pro výpočet
        val latestKm: BigDecimal? = odoRepo.findTopByBike_IdOrderByAtDateDesc(bikeId)?.km

        // načti komponenty bezpečně přes JPQL reader
        val items: List<BikeComponent> = reader.findForBike(bikeId, activeOnly)

        val body = items.map { c: BikeComponent ->
            val lifespan = getLifespanKm(c)
            val used = computeUsedKm(c, latestKm)
            val rem = if (lifespan != null && used != null) {
                val r = lifespan.subtract(used)
                if (r.signum() < 0) BigDecimal.ZERO else r
            } else null
            val pct = if (lifespan != null && lifespan.signum() > 0 && used != null) {
                used.multiply(BigDecimal(100)).divide(lifespan, 2, RoundingMode.HALF_UP)
            } else null

            ComponentWithMetricsResponse(
                id = c.id!!,
                typeKey = c.typeKey,
                typeName = c.typeName,
                label = c.label,
                position = c.position.name,
                installedAt = c.installedAt,
                installedOdometerKm = c.installedOdometerKm,
                lifespanOverride = c.lifespanOverride,
                price = c.price,
                currency = c.currency,
                shop = c.shop,
                receiptPhotoUrl = c.receiptPhotoUrl,
                removedAt = c.removedAt,
                usedKm = used,
                remainingKm = rem,
                percentUsed = pct,
                dueReplacement = rem?.compareTo(BigDecimal.ZERO) == 0
            )
        }

        return ResponseEntity.ok(body)
    }

    private fun getLifespanKm(c: BikeComponent): BigDecimal? {
        c.lifespanOverride?.let { return it }
        val ct = c.typeKey?.let { compTypes.findByKey(it) } ?: return null
        val unit = ct.unit?.lowercase()
        if (unit != "km") return null
        return ct.defaultLifespan
    }

    private fun computeUsedKm(c: BikeComponent, latestKm: BigDecimal?): BigDecimal? {
        val start = c.installedOdometerKm ?: return null
        val latest = latestKm ?: return null
        val used = latest.subtract(start)
        return if (used.signum() < 0) BigDecimal.ZERO else used
    }
}
