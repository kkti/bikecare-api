package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.bikecomponent.history.ComponentEvent
import com.bikecare.bikecomponent.history.ComponentEventRepository
import com.bikecare.bikecomponent.history.EventType
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

@Tag(name = "Bike components")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class BikeComponentController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository,
    private val repo: BikeComponentRepository,
    private val events: ComponentEventRepository
) {

    data class CreateComponentRequest(
        @field:NotBlank val typeKey: String,
        @field:NotBlank val typeName: String,
        val label: String? = null,
        val position: Position? = null,
        val installedAt: Instant? = null,
        @field:PositiveOrZero val installedOdometerKm: BigDecimal? = null,
        @field:PositiveOrZero val lifespanOverride: BigDecimal? = null,
        @field:PositiveOrZero val price: BigDecimal? = null,
        val currency: String? = null,
        val shop: String? = null,
        val receiptPhotoUrl: String? = null,
    )

    data class ComponentResponse(
        val id: Long,
        val typeKey: String,
        val typeName: String,
        val label: String?,
        val position: Position,
        val installedAt: Instant,
        val installedOdometerKm: BigDecimal?,
        val lifespanOverride: BigDecimal?,
        val price: BigDecimal?,
        val currency: String?,
        val shop: String?,
        val receiptPhotoUrl: String?,
        val removedAt: Instant?
    )

    private fun userId(principal: UserDetails) =
        users.findByEmail(principal.username)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

    private fun requireOwnedBike(bikeId: Long, ownerId: Long) {
        val exists = bikes.findByIdAndOwnerId(bikeId, ownerId).isPresent
        if (!exists) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
    }

    private fun toDto(c: BikeComponent) = ComponentResponse(
        id = c.id!!,
        typeKey = c.typeKey,
        typeName = c.typeName,
        label = c.label,
        position = c.position,
        installedAt = c.installedAt,
        installedOdometerKm = c.installedOdometerKm,
        lifespanOverride = c.lifespanOverride,
        price = c.price,
        currency = c.currency,
        shop = c.shop,
        receiptPhotoUrl = c.receiptPhotoUrl,
        removedAt = c.removedAt
    )

    @Operation(summary = "List components for a bike (activeOnly=true => only not-removed)")
    @GetMapping
    fun list(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "true") activeOnly: Boolean
    ): ResponseEntity<List<ComponentResponse>> {
        val uid = userId(principal)
        requireOwnedBike(bikeId, uid)
        val items = if (activeOnly) repo.findAllActiveByBikeId(bikeId, uid)
        else repo.findAllByBikeId(bikeId, uid)
        return ResponseEntity.ok(items.map(::toDto))
    }

    @Operation(summary = "Install (create) a component")
    @PostMapping
    fun create(
        @PathVariable bikeId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: CreateComponentRequest
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val bike = bikes.findByIdAndOwnerId(bikeId, uid).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
        }

        val comp = BikeComponent(
            bike = bike,
            typeKey = body.typeKey,
            typeName = body.typeName,
            label = body.label,
            position = body.position ?: Position.REAR,
            installedAt = body.installedAt ?: Instant.now(),
            installedOdometerKm = body.installedOdometerKm,
            lifespanOverride = body.lifespanOverride,
            price = body.price,
            currency = body.currency,
            shop = body.shop,
            receiptPhotoUrl = body.receiptPhotoUrl
        )
        val saved = repo.save(comp)

        // Uložení události – **použij pojmenované parametry** (bezpečné vůči pořadí)
        events.save(
            ComponentEvent(
                componentId = saved.id!!,
                bikeId = bike.id!!,
                userId = uid,
                eventType = EventType.INSTALLED,
                atTime = saved.installedAt,
                odometerKm = saved.installedOdometerKm
            )
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved))
    }

    @Operation(summary = "Get single component")
    @GetMapping("/{componentId}")
    fun getOne(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val comp = repo.findByIdAndBikeId(componentId, bikeId, uid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")
        return ResponseEntity.ok(toDto(comp))
    }

    @Operation(summary = "Update component meta")
    @PutMapping("/{componentId}")
    fun update(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: CreateComponentRequest
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val comp = repo.findByIdAndBikeId(componentId, bikeId, uid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")

        comp.typeKey = body.typeKey
        comp.typeName = body.typeName
        comp.label = body.label
        comp.position = body.position ?: comp.position
        comp.installedAt = body.installedAt ?: comp.installedAt
        comp.installedOdometerKm = body.installedOdometerKm
        comp.lifespanOverride = body.lifespanOverride
        comp.price = body.price
        comp.currency = body.currency
        comp.shop = body.shop
        comp.receiptPhotoUrl = body.receiptPhotoUrl

        val saved = repo.save(comp)
        return ResponseEntity.ok(toDto(saved))
    }
}
