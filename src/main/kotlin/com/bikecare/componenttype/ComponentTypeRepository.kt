package com.bikecare.componenttype

import org.springframework.data.jpa.repository.JpaRepository

interface ComponentTypeRepository : JpaRepository<ComponentType, Long> {
    // najde záznam podle pole `key` v entitě ComponentType
    fun findByKey(key: String): ComponentType?

    // (volitelné) hodí se při validaci formulářů:
    // fun existsByKey(key: String): Boolean
}
