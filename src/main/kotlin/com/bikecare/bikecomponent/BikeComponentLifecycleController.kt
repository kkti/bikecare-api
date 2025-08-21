package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Components")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components")
class BikeComponentLifecycleController(
    private val bikes: BikeRepository,
    private val comps: BikeComponentRepository,
    private val users: AppUserRepository
) {

    @Operation(summary = "Restore a soft-removed component")
    @PostMapping("/{componentId}/restore")
    fun restore(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        // ověření vlastnictví kola
        bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        val bc = comps.findByIdAndBikeId(componentId, bikeId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found") }

        if (bc.removedAt == null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Component is not removed")
        }

        bc.removedAt = null
        comps.save(bc)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Hard-delete a component (requires it to be soft-removed first)")
    @DeleteMapping("/{componentId}", params = ["hard=true"])
    fun hardDelete(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        bikes.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        val bc = comps.findByIdAndBikeId(componentId, bikeId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found") }

        if (bc.removedAt == null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Component must be soft-removed first")
        }

        comps.delete(bc)
        return ResponseEntity.noContent().build()
    }
}
