package com.bikecare.componenttype

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/component-types")
class ComponentTypeController(private val repo: ComponentTypeRepository) {

    @GetMapping
    fun list(): List<ComponentType> = repo.findAll().sortedBy { it.name }
}
