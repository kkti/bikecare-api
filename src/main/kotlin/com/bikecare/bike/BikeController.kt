package com.bikecare.bike

import com.bikecare.user.AppUserRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

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

@RestController
@RequestMapping("/api/bikes")
class BikeController(
    private val bikes: BikeRepository,
    private val users: AppUserRepository
) {
    /** List vlastních kol */
    @GetMapping
    fun list(@AuthenticationPrincipal principal: UserDetails): List<BikeDto> {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        return bikes.findAllByOwner(user).map { it.toDto() }
    }

    /** Vytvoření kola */
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserDetails,
        @Valid @RequestBody body: BikeDto
    ): BikeDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
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

    /** Detail kola (jen vlastníka) */
    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long
    ): BikeDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findByIdAndOwnerId(id, user.id!!) // vlastnictví v dotazu
            .orElseThrow { NoSuchElementException() }
        return bike.toDto()
    }

    /** Úprava kola (full update) */
    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long,
        @Valid @RequestBody body: UpdateBikeRequest
    ): BikeDto {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findByIdAndOwnerId(id, user.id!!)
            .orElseThrow { NoSuchElementException() }

        bike.name = body.name
        bike.brand = body.brand
        bike.model = body.model
        bike.type = body.type

        val saved = bikes.save(bike)
        return saved.toDto()
    }

    /** Smazání kola (jen vlastníka) */
    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val user = users.findByEmail(principal.username) ?: throw NoSuchElementException()
        val bike = bikes.findByIdAndOwnerId(id, user.id!!)
            .orElseThrow { NoSuchElementException() }

        bikes.delete(bike) // pokud v DB brání FK (komponenty), vrátí 409 přes GlobalExceptionHandler
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
