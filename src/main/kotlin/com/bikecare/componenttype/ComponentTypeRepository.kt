package com.bikecare.componenttype

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ComponentTypeRepository : JpaRepository<ComponentType, Long> {
    fun findByKeyIgnoreCase(key: String): ComponentType?
    fun findByNameIgnoreCase(name: String): ComponentType?
}
