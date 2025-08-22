package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.bikecomponent.history.ComponentEvent
import com.bikecare.bikecomponent.history.ComponentEventRepository
import com.bikecare.bikecomponent.history.EventType
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

@Service
class BikeComponentService(
    private val bikes: BikeRepository,
    private val components: BikeComponentRepository,
    private val events: ComponentEventRepository
) {

    private fun loadOwnedComponent(bikeId: Long, componentId: Long, ownerId: Long): BikeComponent {
        return components.findByIdAndBikeId(componentId, bikeId, ownerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")
    }

    /**
     * Soft delete = označení odstranění (nastaví removedAt), komponenta zůstává v DB.
     */
    @Transactional
    fun softDelete(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant = Instant.now(),
        odometerKm: BigDecimal? = null
    ): BikeComponent {
        val comp = loadOwnedComponent(bikeId, componentId, ownerId)
        if (comp.removedAt == null) {
            comp.removedAt = at
            components.save(comp)
            events.save(
                ComponentEvent(
                    componentId = comp.id!!,
                    bikeId = bikeId,
                    userId = ownerId,
                    eventType = EventType.REMOVED,
                    atTime = at,
                    odometerKm = odometerKm
                )
            )
        }
        return comp
    }

    /**
     * Obnovení (undo soft delete) = vyčistí removedAt.
     */
    @Transactional
    fun restore(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant = Instant.now()
    ): BikeComponent {
        val comp = loadOwnedComponent(bikeId, componentId, ownerId)
        if (comp.removedAt != null) {
            comp.removedAt = null
            components.save(comp)
            events.save(
                ComponentEvent(
                    componentId = comp.id!!,
                    bikeId = bikeId,
                    userId = ownerId,
                    eventType = EventType.RESTORED,
                    atTime = at,
                    odometerKm = null
                )
            )
        }
        return comp
    }

    /**
     * Hard delete = smazání komponenty z DB + smazání jejích eventů.
     */
    @Transactional
    fun hardDelete(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant = Instant.now()
    ) {
        val comp = loadOwnedComponent(bikeId, componentId, ownerId)

        // audit event (dobrovolné – necháváme stopu, že došlo k hard delete)
        events.save(
            ComponentEvent(
                componentId = comp.id!!,
                bikeId = bikeId,
                userId = ownerId,
                eventType = EventType.HARD_DELETED,
                atTime = at,
                odometerKm = null
            )
        )

        // smaž historii této komponenty a následně komponentu
        events.deleteByComponentIdAndUserId(componentId, ownerId)
        components.delete(comp)
    }

    /**
     * Aktualizace instalačních údajů (příklad metody, co loguje UPDATED).
     * Volitelné – používej jen pokud z ní někde voláš.
     */
    @Transactional
    fun updateInstallationInfo(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        installedAt: Instant,
        installedOdometerKm: BigDecimal?
    ): BikeComponent {
        val comp = loadOwnedComponent(bikeId, componentId, ownerId)
        comp.installedAt = installedAt
        comp.installedOdometerKm = installedOdometerKm
        val saved = components.save(comp)

        events.save(
            ComponentEvent(
                componentId = saved.id!!,
                bikeId = bikeId,
                userId = ownerId,
                eventType = EventType.UPDATED,
                atTime = installedAt,
                odometerKm = installedOdometerKm
            )
        )
        return saved
    }
}
