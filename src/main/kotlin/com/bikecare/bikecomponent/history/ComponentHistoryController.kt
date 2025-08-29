package com.bikecare.bikecomponent.history

import com.bikecare.bikecomponent.BikeComponentRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

data class ComponentEventDto(
    val id: Long,
    val eventType: EventType,
    val atTime: Instant,
    val odometerKm: BigDecimal?
)

@Tag(name = "Component history")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components/{componentId}/history")
class ComponentHistoryController(
    private val users: AppUserRepository,
    private val components: BikeComponentRepository,
    private val events: ComponentEventRepository
) {

    private fun ownerId(principal: UserDetails): Long =
        users.findByEmail(principal.username)?.id
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

    @Operation(summary = "List component history (INSTALLED/REMOVED/RESTORED/...) in chronological order")
    @GetMapping
    fun list(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<List<ComponentEventDto>> {
        val uid = ownerId(principal)

        // 404, pokud komponenta nepatří uživateli / kolu
        components.findByIdAndBikeId(componentId, bikeId, uid)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")

        val history = events.findHistoryOwned(bikeId, componentId, uid)
            .map { e -> ComponentEventDto(
                id = e.id!!,
                eventType = e.eventType,
                atTime = e.atTime,
                odometerKm = e.odometerKm
            )}
        return ResponseEntity.ok(history)
    }
}
