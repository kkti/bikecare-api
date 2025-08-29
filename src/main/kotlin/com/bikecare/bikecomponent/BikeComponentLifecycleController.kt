package com.bikecare.bikecomponent

import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

@Tag(name = "Bike components lifecycle")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components/{componentId}")
class BikeComponentLifecycleController(
    private val service: BikeComponentService,
    private val users: AppUserRepository,
) {

    data class ChangeRequest(
        val at: Instant? = null,
        val odometerKm: BigDecimal? = null,
    )

    data class ReplaceRequest(
        val at: Instant? = null,
        val odometerKm: BigDecimal? = null,
        val label: String? = null,
        val price: BigDecimal? = null,
        val currency: String? = null,
        val lifespanOverride: BigDecimal? = null,
        val shop: String? = null,
        val receiptPhotoUrl: String? = null
    )

    data class ReplaceResponse(val newComponentId: Long)

    private fun ownerId(principal: UserDetails): Long =
        users.findByEmail(principal.username)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

    @Operation(summary = "Soft delete (mark component as removed)")
    @DeleteMapping
    fun softDelete(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestBody(required = false) body: ChangeRequest?
    ): ResponseEntity<Void> {
        val uid = ownerId(principal)
        service.softDelete(
            bikeId = bikeId,
            componentId = componentId,
            ownerId = uid,
            at = body?.at ?: Instant.now(),
            odometerKm = body?.odometerKm
        )
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Restore (undo soft delete)")
    @PostMapping("/restore")
    fun restore(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestBody(required = false) body: ChangeRequest?
    ): ResponseEntity<Void> {
        val uid = ownerId(principal)
        service.restore(
            bikeId = bikeId,
            componentId = componentId,
            ownerId = uid,
            at = body?.at ?: Instant.now()
        )
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Hard delete (delete component and its history)")
    @DeleteMapping("/hard")
    fun hardDelete(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @RequestBody(required = false) body: ChangeRequest?
    ): ResponseEntity<Void> {
        val uid = ownerId(principal)
        service.hardDelete(
            bikeId = bikeId,
            componentId = componentId,
            ownerId = uid,
            at = body?.at ?: Instant.now()
        )
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Replace component (close old → create new of the same type)")
    @PostMapping("/replace")
    fun replace(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody(required = false) body: ReplaceRequest?
    ): ResponseEntity<ReplaceResponse> {
        val uid = ownerId(principal)
        val at = body?.at ?: Instant.now()
        val newComp = service.replace(
            bikeId = bikeId,
            componentId = componentId,
            ownerId = uid,
            at = at,
            odometerKm = body?.odometerKm,
            newLabel = body?.label,
            newPrice = body?.price,
            newCurrency = body?.currency,
            newLifespanOverride = body?.lifespanOverride,
            newShop = body?.shop,
            newReceiptPhotoUrl = body?.receiptPhotoUrl
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(ReplaceResponse(newComp.id!!))
    }
}
