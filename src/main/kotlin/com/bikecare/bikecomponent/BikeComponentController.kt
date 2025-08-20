package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.componenttype.ComponentTypeRepository
import com.bikecare.user.AppUserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant

data class InstallComponentRequest(
    val typeKey: String, val label: String?, val position: String?,
    val installedAt: Instant? = null, val installedOdometerKm: Double? = null,
    val lifespanOverride: Double? = null, val price: Double? = null,
    val currency: String? = null, val shop: String? = null
)
data class ComponentDto(
    val id: Long, val typeKey: String, val typeName: String,
    val label: String?, val position: String?, val installedAt: Instant,
    val installedOdometerKm: Double?, val lifespanOverride: Double?
)

@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class BikeComponentController(
    private val bikes: BikeRepository,
    private val types: ComponentTypeRepository,
    private val comps: BikeComponentRepository,
    private val users: AppUserRepository
) {
    @GetMapping
    fun list(@PathVariable bikeId: Long, @AuthenticationPrincipal principal: UserDetails,
             @RequestParam(defaultValue = "true") active: Boolean): List<ComponentDto> {
        val u = users.findByEmail(principal.username).orElseThrow()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == u.id) { "Forbidden" }

        val items = if (active) comps.findAllByBikeIdAndRemovedAtIsNull(bikeId)
        else comps.findAll().filter { it.bike.id == bikeId }
        return items.map {
            ComponentDto(
                id = it.id!!, typeKey = it.type.key, typeName = it.type.name,
                label = it.label, position = it.position, installedAt = it.installedAt,
                installedOdometerKm = it.installedOdometerKm?.toDouble(),
                lifespanOverride = it.lifespanOverride?.toDouble()
            )
        }
    }

    @PostMapping
    fun install(@PathVariable bikeId: Long, @AuthenticationPrincipal principal: UserDetails,
                @RequestBody body: InstallComponentRequest): ResponseEntity<ComponentDto> {
        val u = users.findByEmail(principal.username).orElseThrow()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == u.id) { "Forbidden" }

        val type = types.findByKey(body.typeKey) ?: return ResponseEntity.badRequest().build()
        val saved = comps.save(
            BikeComponent(
                bike = bike, type = type, label = body.label, position = body.position,
                installedAt = body.installedAt ?: Instant.now(),
                installedOdometerKm = body.installedOdometerKm?.let { BigDecimal.valueOf(it) },
                lifespanOverride = body.lifespanOverride?.let { BigDecimal.valueOf(it) },
                price = body.price?.let { BigDecimal.valueOf(it) },
                currency = body.currency, shop = body.shop
            )
        )
        return ResponseEntity.ok(
            ComponentDto(saved.id!!, type.key, type.name, saved.label, saved.position,
                saved.installedAt, saved.installedOdometerKm?.toDouble(), saved.lifespanOverride?.toDouble())
        )
    }

    @PostMapping("/{componentId}/remove")
    fun remove(@PathVariable bikeId: Long, @PathVariable componentId: Long,
               @AuthenticationPrincipal principal: UserDetails): ResponseEntity<Void> {
        val u = users.findByEmail(principal.username).orElseThrow()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == u.id) { "Forbidden" }

        val bc = comps.findById(componentId).orElseThrow()
        require(bc.bike.id == bike.id) { "Forbidden" }
        bc.removedAt = Instant.now()
        comps.save(bc)
        return ResponseEntity.noContent().build()
    }
}
