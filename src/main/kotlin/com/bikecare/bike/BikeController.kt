package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

data class BikeDto(
    val id: Long?,
    @field:NotBlank(message = "name is required")
    val name: String,
    val brand: String?,
    val model: String?,
    val type: String?
)

data class UpdateBikeRequest(
    @field:NotBlank(message = "name is required")
    val name: String,
    val brand: String?,
    val model: String?,
    val type: String?
)

@Tag(name = "Bikes")
@RestController
@RequestMapping("/api/bikes")
class BikeController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository
) {
    @GetMapping
    fun list(@Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails): List<BikeDto> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        return bikes.findAllByOwner(user).map { it.toDto() }
    }

    @PostMapping
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: BikeDto
    ): BikeDto {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val saved = bikes.save(
            Bike(
                owner = user,
                name = body.name,
                brand = body.brand,
                model = body.model,
                type = body.type
            )
        )
        return saved.toDto()
    }

    @GetMapping("/{id}")
    fun detail(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long
    ): BikeDto {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val bike = bikes.findByIdAndOwnerId(id, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }
        return bike.toDto()
    }

    @PutMapping("/{id}")
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody body: UpdateBikeRequest
    ): BikeDto {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val bike = bikes.findByIdAndOwnerId(id, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        bike.name = body.name
        bike.brand = body.brand
        bike.model = body.model
        bike.type = body.type

        val saved = bikes.save(bike)
        return saved.toDto()
    }

    @DeleteMapping("/{id}")
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        val bike = bikes.findByIdAndOwnerId(id, user.id!!)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        bikes.delete(bike)
        return ResponseEntity.noContent().build()
    }
}

private fun Bike.toDto() = BikeDto(
    id = this.id,
    name = this.name,
    brand = this.brand,
    model = this.model,
    type = this.type
)
