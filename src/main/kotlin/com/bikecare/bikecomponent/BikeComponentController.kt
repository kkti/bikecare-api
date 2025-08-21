package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.componenttype.ComponentTypeRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant

data class InstallComponentRequest(
    @field:NotBlank(message = "typeKey is required")
    val typeKey: String,
    val label: String?,
    @field:Pattern(
        regexp = "FRONT|REAR|LEFT|RIGHT",
        message = "position must be one of FRONT, REAR, LEFT, RIGHT"
    )
    val position: String?,
    val installedAt: Instant?,
    val installedOdometerKm: Double?,
    val lifespanOverride: Double?,
    val price: Double?,
    val currency: String?,
    val shop: String?,
    val receiptPhotoUrl: String?
)

data class ComponentDto(
    val id: Long,
    val typeKey: String,
    val typeName: String,
    val label: String?,
    val position: String?,
    val installedAt: Instant?,
    val installedOdometerKm: Double?,
    val lifespanOverride: Double?,
    val price: Double?,
    val currency: String?,
    val shop: String?,
    val receiptPhotoUrl: String?,
    val removedAt: Instant?
)

@Tag(name = "Components")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class BikeComponentController(
    private val bikes: BikeRepository,
    private val comps: BikeComponentRepository,
    private val types: ComponentTypeRepository,
    private val users: AppUserRepository
) {

    @GetMapping
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @RequestParam(required = false, defaultValue = "true") active: Boolean
    ): List<ComponentDto> {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == user.id) { "Forbidden" }

        val list = if (active) comps.findAllActiveByBikeId(bikeId) else comps.findAllByBikeId(bikeId)
        return list.map { it.toDto() }
    }

    @PostMapping
    fun install(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @Valid @RequestBody body: InstallComponentRequest
    ): ComponentDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == user.id) { "Forbidden" }

        val type = types.findByKey(body.typeKey) ?: throw NoSuchElementException("Unknown component type: ${body.typeKey}")

        val entity = BikeComponent(
            bike = bike,
            type = type,
            label = body.label,
            position = body.position,
            installedAt = body.installedAt ?: Instant.now(),
            installedOdometerKm = body.installedOdometerKm?.let { BigDecimal.valueOf(it) },
            lifespanOverride = body.lifespanOverride?.let { BigDecimal.valueOf(it) },
            price = body.price?.let { BigDecimal.valueOf(it) },
            currency = body.currency,
            shop = body.shop,
            receiptPhotoUrl = body.receiptPhotoUrl
        )
        val saved = comps.save(entity)
        return saved.toDto()
    }

    @PostMapping("/{componentId}/remove")
    fun remove(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == user.id) { "Forbidden" }

        val bc = comps.findById(componentId).orElseThrow()
        require(bc.bike?.id == bike.id) { "Forbidden" }

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
    lifespanOverride = this.lifespanOverride?.toDouble(),
    price = this.price?.toDouble(),
    currency = this.currency,
    shop = this.shop,
    receiptPhotoUrl = this.receiptPhotoUrl,
    removedAt = this.removedAt
)
