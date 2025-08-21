package com.bikecare.componenttype


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/component-types")
@Tag(name = "Component Types", description = "Katalog dostupných typů komponent pro klientské formuláře")
class ComponentTypeController(private val repo: ComponentTypeRepository) {


    @GetMapping
    @Cacheable("componentTypes")
    @Operation(
        summary = "Seznam typů komponent",
        description = "Vrací katalog typů komponent pro formuláře (např. řetěz, kazeta, kotouč…). Seřazeno podle názvu.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "OK",
                content = [Content(mediaType = "application/json",
                    schema = Schema(implementation = ComponentTypeDto::class))])
        ]
    )
    fun list(): List<ComponentTypeDto> = repo.findAll()
        .sortedBy { it.name }
        .map { it.toDto() }
}