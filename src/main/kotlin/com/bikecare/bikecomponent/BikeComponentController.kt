package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.bikecomponent.history.ComponentEvent
import com.bikecare.bikecomponent.history.ComponentEventRepository
import com.bikecare.bikecomponent.history.EventType
import com.bikecare.componenttype.ComponentTypeRepository
import com.bikecare.config.HealthProperties
import com.bikecare.odometer.OdometerRepository
import com.bikecare.user.AppUserRepository
import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import java.time.ZoneOffset
import kotlin.math.max
import kotlin.math.min

@Tag(name = "Bike components")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class BikeComponentController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository,
    private val repo: BikeComponentRepository,
    private val events: ComponentEventRepository,
    private val componentTypes: ComponentTypeRepository,
    private val odometers: OdometerRepository,
    private val health: HealthProperties, // default prahy z YAML
) {

    data class CreateComponentRequest(
        @field:NotBlank
        @JsonAlias("typeName")
        val typeKey: String,
        val typeName: String? = null,
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
        val removedAt: Instant?,
        val lifespanKm: BigDecimal?,
        val usedKm: BigDecimal?,
        val remainingKm: BigDecimal?,
        val percentWear: Int?,
        val status: String? = null
    )

    data class PageResponse<T>(
        val content: List<T>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
        val sort: String
    )

    private fun userId(principal: UserDetails) =
        users.findByEmail(principal.username)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

    private fun requireOwnedBike(bikeId: Long, ownerId: Long) {
        val exists = bikes.findByIdAndOwnerId(bikeId, ownerId).isPresent
        if (!exists) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
    }

    private fun baseDto(c: BikeComponent) = ComponentResponse(
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
        removedAt = c.removedAt,
        lifespanKm = null,
        usedKm = null,
        remainingKm = null,
        percentWear = null,
        status = null
    )

    private fun withMetrics(
        dto: ComponentResponse,
        comp: BikeComponent,
        currentOdometerKm: BigDecimal?,
        bikeIdFallback: Long? = null
    ): ComponentResponse {
        val bikeIdForLookup: Long? = bikeIdFallback ?: comp.bike.id
        val removedAt = comp.removedAt // smart-cast friendly

        val effectiveCurrentKm: BigDecimal? = when {
            currentOdometerKm != null -> currentOdometerKm
            removedAt != null && bikeIdForLookup != null -> {
                val date = removedAt.atZone(ZoneOffset.UTC).toLocalDate()
                odometers.findTopByBikeIdAndAtDateLessThanEqualOrderByAtDateDesc(bikeIdForLookup, date)?.km
            }
            bikeIdForLookup != null -> odometers.findTopByBikeIdOrderByAtDateDesc(bikeIdForLookup)?.km
            else -> null
        }

        val defLifespan = componentTypes.findByKeyIgnoreCase(comp.typeKey)?.defaultLifespan
        val metrics = ComponentHealth.compute(
            installedOdometerKm = comp.installedOdometerKm,
            currentOdometerKm = effectiveCurrentKm,
            lifespanOverride = comp.lifespanOverride,
            defaultLifespan = defLifespan
        ) ?: return dto

        return dto.copy(
            lifespanKm = metrics.lifespanKm,
            usedKm = metrics.usedKm,
            remainingKm = metrics.remainingKm,
            percentWear = metrics.percentWear
        )
    }

    /** Jednotná klasifikace přes ComponentHealth.classify – umí i EXPIRED. */
    private fun withStatus(dto: ComponentResponse, warnAt: Int?, criticalAt: Int?): ComponentResponse {
        val w = warnAt ?: health.warnAt
        val c = criticalAt ?: health.criticalAt

        val m = if (dto.lifespanKm != null && dto.usedKm != null && dto.remainingKm != null && dto.percentWear != null) {
            ComponentHealth.Metrics(
                lifespanKm = dto.lifespanKm,
                usedKm = dto.usedKm,
                remainingKm = dto.remainingKm,
                percentWear = dto.percentWear
            )
        } else null

        val status = m?.let { ComponentHealth.classify(it, w, c).name } ?: return dto
        return dto.copy(status = status)
    }

    @Operation(summary = "List components for a bike (activeOnly=true => only not-removed)")
    @GetMapping
    fun list(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(required = false) currentOdometerKm: BigDecimal? = null,
        @RequestParam(required = false) warnAt: Int? = null,
        @RequestParam(required = false) criticalAt: Int? = null
    ): ResponseEntity<List<ComponentResponse>> {
        val uid = userId(principal)
        requireOwnedBike(bikeId, uid)
        val items = if (activeOnly) repo.findAllActiveByBikeId(bikeId, uid) else repo.findAllByBikeId(bikeId, uid)
        val result = items.map { comp ->
            val withM = withMetrics(baseDto(comp), comp, currentOdometerKm, bikeId)
            withStatus(withM, warnAt, criticalAt)
        }
        return ResponseEntity.ok(result)
    }

    @Operation(summary = "Install (create) a component")
    @PostMapping
    fun create(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: CreateComponentRequest
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val bike = bikes.findByIdAndOwnerId(bikeId, uid).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found")
        }

        val effectiveTypeKey = body.typeKey
        val effectiveTypeName = body.typeName ?: body.typeKey

        val comp = BikeComponent(
            bike = bike,
            typeKey = effectiveTypeKey,
            typeName = effectiveTypeName,
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

        return ResponseEntity.status(HttpStatus.CREATED).body(baseDto(saved))
    }

    @Operation(summary = "Get single component")
    @GetMapping("/{componentId}")
    fun getOne(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestParam(required = false) currentOdometerKm: BigDecimal? = null,
        @RequestParam(required = false) warnAt: Int? = null,
        @RequestParam(required = false) criticalAt: Int? = null
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val comp = repo.findByIdAndBikeId(componentId, bikeId, uid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")
        val dto = withStatus(
            withMetrics(baseDto(comp), comp, currentOdometerKm, bikeId),
            warnAt, criticalAt
        )
        return ResponseEntity.ok(dto)
    }

    @Operation(summary = "Paged list with filters + optional metrics/status")
    @GetMapping("/page")
    fun page(
        @PathVariable bikeId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,

        @RequestParam(defaultValue = "true") activeOnly: Boolean,
        @RequestParam(required = false) typeKey: String?,
        @RequestParam(required = false) position: Position?,
        @RequestParam(required = false) labelLike: String?,

        @RequestParam(required = false) currentOdometerKm: BigDecimal?,
        @RequestParam(required = false) warnAt: Int?,
        @RequestParam(required = false) criticalAt: Int?,

        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "installedAt,DESC") sort: String
    ): ResponseEntity<PageResponse<ComponentResponse>> {
        val uid = userId(principal)
        requireOwnedBike(bikeId, uid)

        var items = if (activeOnly) repo.findAllActiveByBikeId(bikeId, uid) else repo.findAllByBikeId(bikeId, uid)

        items = items.filter { c ->
            val okType = typeKey?.let { it.equals(c.typeKey, ignoreCase = true) } ?: true
            val okPos = position?.let { it == c.position } ?: true
            val okLabel = labelLike?.let { like -> c.label?.contains(like, ignoreCase = true) ?: false } ?: true
            okType && okPos && okLabel
        }

        val (prop, dirRaw) = (sort.ifBlank { "installedAt,DESC" }).split(",", limit = 2).let {
            (it.getOrNull(0) ?: "installedAt") to (it.getOrNull(1) ?: "DESC")
        }
        val desc = dirRaw.equals("DESC", ignoreCase = true)
        val comparator = when (prop.lowercase()) {
            "typekey" -> compareBy<BikeComponent> { it.typeKey.lowercase() }
            "installedat" -> compareBy<BikeComponent> { it.installedAt }
            "position" -> compareBy<BikeComponent> { it.position.name }
            "label" -> compareBy<BikeComponent> { it.label ?: "" }
            else -> compareBy<BikeComponent> { it.installedAt }
        }
        items = if (desc) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)

        val total = items.size.toLong()
        val from = max(0, page * size)
        val to = min(items.size, from + size)
        val pageSlice = if (from < to) items.subList(from, to) else emptyList()

        val content = pageSlice.map { comp ->
            val withM = withMetrics(baseDto(comp), comp, currentOdometerKm, bikeId)
            withStatus(withM, warnAt, criticalAt)
        }

        val totalPages = if (size <= 0) 1 else ((total + size - 1) / size).toInt()
        val effectiveSort = "${prop},${if (desc) "DESC" else "ASC"}"

        return ResponseEntity.ok(
            PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = total,
                totalPages = totalPages,
                sort = effectiveSort
            )
        )
    }

    @Operation(summary = "Update component meta")
    @PutMapping("/{componentId}")
    fun update(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: CreateComponentRequest
    ): ResponseEntity<ComponentResponse> {
        val uid = userId(principal)
        val comp = repo.findByIdAndBikeId(componentId, bikeId, uid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")

        val effectiveTypeKey = body.typeKey
        val effectiveTypeName = body.typeName ?: body.typeKey

        comp.typeKey = effectiveTypeKey
        comp.typeName = effectiveTypeName
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
        return ResponseEntity.ok(baseDto(saved))
    }
}
