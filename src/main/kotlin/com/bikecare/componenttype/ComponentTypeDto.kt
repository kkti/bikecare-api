package com.bikecare.componenttype


import java.math.BigDecimal


data class ComponentTypeDto(
    val id: Long,
    val key: String,
    val name: String,
    val unit: String,
    val defaultLifespan: BigDecimal?,
    val defaultServiceInterval: BigDecimal?
)


fun ComponentType.toDto() = ComponentTypeDto(
    id = this.id!!,
    key = this.key,
    name = this.name,
    unit = this.unit,
    defaultLifespan = this.defaultLifespan,
    defaultServiceInterval = this.defaultServiceInterval
)