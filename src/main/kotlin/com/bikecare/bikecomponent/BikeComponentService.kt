package com.bikecare.bikecomponent

import com.bikecare.bike.BikeRepository
import com.bikecare.bikecomponent.history.ComponentEvent
import com.bikecare.bikecomponent.history.ComponentEventRepository
import com.bikecare.bikecomponent.history.EventType
import com.bikecare.odometer.OdometerRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

@Service
class BikeComponentService(
    private val bikes: BikeRepository,
    private val repo: BikeComponentRepository,
    private val events: ComponentEventRepository,
    private val odometers: OdometerRepository
) {

    private fun ownedComponentOr404(bikeId: Long, componentId: Long, ownerId: Long): BikeComponent =
        repo.findByIdAndBikeId(componentId, bikeId, ownerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found")

    @Transactional
    fun softDelete(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant,
        odometerKm: BigDecimal?
    ) {
        val comp = ownedComponentOr404(bikeId, componentId, ownerId)
        if (comp.removedAt == null) {
            comp.removedAt = at
            repo.save(comp)
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
    }

    @Transactional
    fun restore(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant
    ) {
        val comp = ownedComponentOr404(bikeId, componentId, ownerId)
        if (comp.removedAt != null) {
            comp.removedAt = null
            repo.save(comp)
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
    }

    @Transactional
    fun hardDelete(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant
    ) {
        val comp = ownedComponentOr404(bikeId, componentId, ownerId)
        // smaž historii a komponentu
        events.deleteByComponentId(comp.id!!)
        repo.delete(comp)
    }

    /**
     * Výměna komponenty: starou označí REMOVED, vytvoří novou se stejným typem.
     * Vrací nově vytvořenou komponentu.
     */
    @Transactional
    fun replace(
        bikeId: Long,
        componentId: Long,
        ownerId: Long,
        at: Instant,
        odometerKm: BigDecimal?,
        newLabel: String?,
        newPrice: BigDecimal?,
        newCurrency: String?,
        newLifespanOverride: BigDecimal?,
        newShop: String?,
        newReceiptPhotoUrl: String?
    ): BikeComponent {
        // validace vlastnictví + existence kola
        bikes.findByIdAndOwnerId(bikeId, ownerId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bike not found") }

        val old = ownedComponentOr404(bikeId, componentId, ownerId)

        // 1) uzavři starou
        if (old.removedAt == null) {
            old.removedAt = at
            repo.save(old)
            events.save(
                ComponentEvent(
                    componentId = old.id!!,
                    bikeId = bikeId,
                    userId = ownerId,
                    eventType = EventType.REMOVED,
                    atTime = at,
                    odometerKm = odometerKm
                )
            )
        }

        // 2) určete km instalace nové
        val latestKm = odometerKm
            ?: odometers.findTopByBikeIdOrderByAtDateDesc(bikeId)?.km
            ?: old.installedOdometerKm

        // 3) vytvoř novou se stejným typem a pozicí
        val fresh = BikeComponent(
            bike = old.bike,
            typeKey = old.typeKey,
            typeName = old.typeName,
            label = newLabel ?: old.label,
            position = old.position,
            installedAt = at,
            installedOdometerKm = latestKm,
            lifespanOverride = newLifespanOverride ?: old.lifespanOverride,
            price = newPrice ?: old.price,
            currency = newCurrency ?: old.currency,
            shop = newShop ?: old.shop,
            receiptPhotoUrl = newReceiptPhotoUrl ?: old.receiptPhotoUrl
        )
        val saved = repo.save(fresh)

        // 4) event INSTALLED pro novou
        events.save(
            ComponentEvent(
                componentId = saved.id!!,
                bikeId = bikeId,
                userId = ownerId,
                eventType = EventType.INSTALLED,
                atTime = at,
                odometerKm = latestKm
            )
        )

        return saved
    }
}
