package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.componenttype.ComponentTypeRepository
import com.bikecare.user.AppUserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
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
        val user = users.findByEmail(principal.username).orElseThrow()
        val bike = bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

        val items = if (active)
            comps.findAllByBikeIdAndRemovedAtIsNull(bike.id!!)
        else
            comps.findAllByBikeId(bike.id!!)

        return items.map { it.toDto() }
    }

    @PostMapping
    @Transactional
    fun install(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody body: InstallComponentRequest
    ): ResponseEntity<ComponentDto> {
        val user = users.findByEmail(principal.username).orElseThrow()
        val bike = bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN) }

        val type = types.findByKey(body.typeKey)
            ?: return ResponseEntity.badRequest().build()

        val pos = body.position?.uppercase()?.let {
            when (it) {
                "FRONT", "REAR", "LEFT", "RIGHT" -> it
                else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid position '$it'")
            }
        }

        val saved = comps.save(
            BikeComponent(
                bike = bike,
                type = type,
                label = body.label,
                position = pos,
                installedAt = body.installedAt ?: Instant.now(),
                installedOdometerKm = body.installedOdometerKm?.let(BigDecimal::valueOf),
                lifespanOverride = body.lifespanOverride?.let(BigDecimal::valueOf),
                price = body.price?.let(BigDecimal::valueOf),
                currency = body.currency,
                shop = body.shop
            )
        )
        return ResponseEntity.ok(saved.toDto())
    }

    @PostMapping("/{componentId}/remove")
    @Transactional
    fun remove(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username).orElseThrow()
        // najdeme kolo patřící userovi a zároveň komponentu patřící tomu kolu
        bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN) }

        val bc = comps.findByIdAndBikeId(componentId, bikeId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

        bc.removedAt = Instant.now()
        comps.save(bc)
        return ResponseEntity.noContent().build()
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
}
