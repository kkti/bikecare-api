package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

data class BikeDto(val id: Long?, val name: String, val brand: String?, val model: String?, val type: String?)

@RestController
@RequestMapping("/api/bikes")
class BikeController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: UserDetails): List<BikeDto> {
        val u = users.findByEmail(principal.username).orElseThrow()
        return bikes.findAllByOwnerId(u.id!!).map { BikeDto(it.id, it.name, it.brand, it.model, it.type) }
    }

    @PostMapping
    fun create(@AuthenticationPrincipal principal: UserDetails, @RequestBody body: BikeDto): BikeDto {
        val u = users.findByEmail(principal.username).orElseThrow()
        val saved = bikes.save(Bike(owner = u, name = body.name, brand = body.brand, model = body.model, type = body.type))
        return BikeDto(saved.id, saved.name, saved.brand, saved.model, saved.type)
    }
}
