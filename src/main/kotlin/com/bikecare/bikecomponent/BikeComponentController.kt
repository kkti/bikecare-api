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
    val typeKey: String,
    val label: String?,
    val position: String?,
    val installedAt: Instant? = null,
    val installedOdometerKm: Double? = null,
    val lifespanOverride: Double? = null,
    val price: Double? = null,
    val currency: String? = null,
    val shop: String? = null
)

data class ComponentDto(
    val id: Long,
    val typeKey: String,
    val typeName: String,
    val label: String?,
    val position: String?,
    val installedAt: Instant,
    val installedOdometerKm: Double?,
    val lifespanOverride: Double?
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
    fun list(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "true") active: Boolean
    ): List<ComponentDto> {
        val userId = users.findByEmail(principal.username).orElseThrow().id!!
        // 404 pokud kolo nepatří uživateli
        bikes.findByIdAndOwner_Id(bikeId, userId).orElseThrow()

        val items = if (active)
            comps.findAllByBike_IdAndRemovedAtIsNull(bikeId)
        else
            comps.findAllByBike_Id(bikeId)

        return items.map { it.toDto() }
    }

    @PostMapping
    fun install(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody body: InstallComponentRequest
    ): ResponseEntity<ComponentDto> {
        val userId = users.findByEmail(principal.username).orElseThrow().id!!
        val bike = bikes.findByIdAndOwner_Id(bikeId, userId).orElseThrow()

        val type = types.findByKey(body.typeKey) ?: return ResponseEntity.badRequest().build()

        val saved = comps.save(
            BikeComponent(
                bike = bike,
                type = type,
                label = body.label,
                position = body.position,
                installedAt = body.installedAt ?: Instant.now(),
                installedOdometerKm = body.installedOdometerKm?.let { BigDecimal.valueOf(it) },
                lifespanOverride = body.lifespanOverride?.let { BigDecimal.valueOf(it) },
                price = body.price?.let { BigDecimal.valueOf(it) },
                currency = body.currency,
                shop = body.shop
            )
        )
        return ResponseEntity.ok(saved.toDto())
    }

    @PostMapping("/{componentId}/remove")
    fun remove(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<Void> {
        val userId = users.findByEmail(principal.username).orElseThrow().id!!
        // ověří vlastnictví kola + že komponenta patří danému kolu
        bikes.findByIdAndOwner_Id(bikeId, userId).orElseThrow()
        val bc = comps.findByIdAndBike_Id(componentId, bikeId).orElseThrow()

        bc.removedAt = Instant.now()
        comps.save(bc)
        return ResponseEntity.noContent().build()
    }
}

private fun BikeComponent.toDto() = ComponentDto(
    id = this.id!!,
    typeKey = this.type.key,
    typeName = this.type.name,
    label = this.label,
    position = this.position,
    installedAt = this.installedAt,
    installedOdometerKm = this.installedOdometerKm?.toDouble(),
    lifespanOverride = this.lifespanOverride?.toDouble()
)
