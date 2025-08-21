package com.bikecare.bikecomponent.history

import com.bikecare.bike.BikeRepository
import com.bikecare.bikecomponent.BikeComponentRepository
import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Tag(name = "Components - History")
@RestController
@RequestMapping("/api/bikes/{bikeId}/components/{componentId}/history")
class ComponentHistoryController(
    private val bikeRepo: BikeRepository,
    private val compRepo: BikeComponentRepository,
    private val eventRepo: ComponentEventRepository,
    private val users: AppUserRepository
) {

    @GetMapping
    @Operation(summary = "Historie instalací/odstranění komponenty (nejnovější první)")
    fun history(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal principal: UserDetails
    ): List<ComponentEvent> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        bikeRepo.findByIdAndOwnerId(bikeId, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        compRepo.findByIdAndBikeId(componentId, bikeId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

        return eventRepo.findHistoryOwned(bikeId, componentId, user.id!!)
    }
}
