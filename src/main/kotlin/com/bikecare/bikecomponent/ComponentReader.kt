package com.bikecare.bikecomponent

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class ComponentReader(
    @PersistenceContext private val em: EntityManager
) {
    /**
     * Vrátí komponenty pro dané kolo. Zkouší nejdřív relaci c.bike.id,
     * když entita nemá relaci, zkusí c.bikeId. Aktivní = removedAt IS NULL.
     */
    fun findForBike(bikeId: Long, activeOnly: Boolean): List<BikeComponent> {
        val activeClause = if (activeOnly) " and c.removedAt IS NULL " else ""
        // 1) pokus přes relaci c.bike.id
        return try {
            em.createQuery(
                "select c from BikeComponent c where c.bike.id = :bikeId$activeClause order by c.installedAt desc",
                BikeComponent::class.java
            ).setParameter("bikeId", bikeId)
             .resultList
        } catch (_: IllegalArgumentException) {
            // 2) fallback: přímé pole c.bikeId
            em.createQuery(
                "select c from BikeComponent c where c.bikeId = :bikeId$activeClause order by c.installedAt desc",
                BikeComponent::class.java
            ).setParameter("bikeId", bikeId)
             .resultList
        }
    }
}
