package com.bikecare.componenttype

import org.springframework.data.jpa.repository.JpaRepository

interface ComponentTypeRepository : JpaRepository<ComponentType, Long> {
    fun findByKey(key: String): ComponentType?
}
