package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

data class BikeDto(
    val id: Long?,
    val name: String,
    val brand: String?,
    val model: String?,
    val type: String?
)

@RestController
@RequestMapping("/api/bikes")
class BikeController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: UserDetails): List<BikeDto> {
        val userId = users.findByEmail(principal.username).orElseThrow().id!!
        return bikes.findAllByOwner_Id(userId).map { it.toDto() }
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @RequestBody body: BikeDto
    ): BikeDto {
        val user = users.findByEmail(principal.username).orElseThrow()
        val saved = bikes.save(Bike(owner = user, name = body.name, brand = body.brand, model = body.model, type = body.type))
        return saved.toDto()
    }
}

private fun Bike.toDto() = BikeDto(id = this.id, name = this.name, brand = this.brand, model = this.model, type = this.type)
