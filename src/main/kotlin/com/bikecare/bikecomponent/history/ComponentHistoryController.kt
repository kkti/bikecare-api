package com.bikecare.bikecomponent.history

import com.bikecare.user.AppUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bikes/{bikeId}/components/{componentId}/history")
class ComponentHistoryController(
    private val events: ComponentEventRepository
) {

    @GetMapping
    fun list(
        @PathVariable bikeId: Long,
        @PathVariable componentId: Long,
        @AuthenticationPrincipal user: AppUser
    ): ResponseEntity<List<ComponentEvent>> {
        val data = events.findHistoryOwned(bikeId, componentId, user.id!!)
        return ResponseEntity.ok(data)
    }
}
