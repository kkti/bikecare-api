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
import kotlin.math.max

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
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO 4217 (3 uppercase letters)")
    val currency: String?,
    val shop: String?,
    val receiptPhotoUrl: String?
)

data class UpdateComponentRequest(
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
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO 4217 (3 uppercase letters)")
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

data class ComponentStatusDto(
    val lifespanTotalKm: Double?,   // override nebo default z type
    val usedKm: Double?,            // currentOdometerKm - installedOdometerKm
    val remainingKm: Double?,       // max(total - used, 0)
    val percentUsed: Double?,       // 0..100
    val serviceIntervalKm: Double?, // default_service_interval z type
    val kmsToService: Double?,      // (installed + interval) - current
    val serviceDue: Boolean?        // kmsToService <= 0
)

data class ComponentDetailDto(
    val component: ComponentDto,
    val status: ComponentStatusDto?
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
            position = body.position?.let { ComponentPos.valueOf(it) },
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

    @GetMapping("/{componentId}")
    fun detail(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @RequestParam(required = false) currentOdometerKm: Double?
    ): ComponentDetailDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == user.id) { "Forbidden" }

        val bc = comps.findByIdAndBikeId(componentId, bikeId).orElseThrow()
        val status = computeStatus(bc, currentOdometerKm)
        return ComponentDetailDto(component = bc.toDto(), status = status)
    }

    @PatchMapping("/{componentId}")
    fun patch(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Valid @RequestBody body: UpdateComponentRequest
    ): ComponentDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findById(bikeId).orElseThrow()
        require(bike.owner?.id == user.id) { "Forbidden" }

        val bc = comps.findByIdAndBikeId(componentId, bikeId).orElseThrow()

        body.label?.let { bc.label = it }
        body.position?.let { bc.position = ComponentPos.valueOf(it) }
        body.installedAt?.let { bc.installedAt = it }
        body.installedOdometerKm?.let { bc.installedOdometerKm = BigDecimal.valueOf(it) }
        body.lifespanOverride?.let { bc.lifespanOverride = BigDecimal.valueOf(it) }
        body.price?.let { bc.price = BigDecimal.valueOf(it) }
        body.currency?.let { bc.currency = it }
        body.shop?.let { bc.shop = it }
        body.receiptPhotoUrl?.let { bc.receiptPhotoUrl = it }

        val saved = comps.save(bc)
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

        val bc = comps.findByIdAndBikeId(componentId, bikeId).orElseThrow()
        bc.removedAt = Instant.now()
        comps.save(bc)
        return ResponseEntity.noContent().build()
    }

    private fun computeStatus(bc: BikeComponent, currentOdometerKm: Double?): ComponentStatusDto? {
        if (currentOdometerKm == null || bc.installedOdometerKm == null) return null

        val installed = bc.installedOdometerKm!!.toDouble()
        val used = max(0.0, currentOdometerKm - installed)

        val total: Double? = when {
            bc.lifespanOverride != null -> bc.lifespanOverride!!.toDouble()
            bc.type.defaultLifespan != null -> bc.type.defaultLifespan!!.toDouble()
            else -> null
        }

        val remaining = total?.let { max(0.0, it - used) }
        val percent = total?.takeIf { it > 0.0 }?.let { (used / it).coerceIn(0.0, 1.0) * 100.0 }

        val interval = bc.type.defaultServiceInterval?.toDouble()
        val kmsToService = interval?.let { (installed + it) - currentOdometerKm }
        val serviceDue = kmsToService?.let { it <= 0.0 }

        return ComponentStatusDto(
            lifespanTotalKm = total,
            usedKm = used,
            remainingKm = remaining,
            percentUsed = percent,
            serviceIntervalKm = interval,
            kmsToService = kmsToService,
            serviceDue = serviceDue
        )
    }
}

private fun BikeComponent.toDto() = ComponentDto(
    id = this.id!!,
    typeKey = this.type.key,
    typeName = this.type.name,
    label = this.label,
    position = this.position?.name,
    installedAt = this.installedAt,
    installedOdometerKm = this.installedOdometerKm?.toDouble(),
    lifespanOverride = this.lifespanOverride?.toDouble(),
    price = this.price?.toDouble(),
    currency = this.currency,
    shop = this.shop,
    receiptPhotoUrl = this.receiptPhotoUrl,
    removedAt = this.removedAt
)
