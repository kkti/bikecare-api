package com.bikecare.servicelog

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.Instant

@RestController
@RequestMapping("/api/bikes/{bikeId}/service-records")
class ServiceRecordController(
    private val repo: ServiceRecordRepository
) {

    data class ServiceRecordRequest(
        val componentId: Long? = null,
        val performedAt: Instant? = null,
        val description: String,
        val kmAtService: BigDecimal? = null,
        val cost: BigDecimal? = null,
        val currency: String? = "CZK",
        val note: String? = null
    )

    @GetMapping
    fun list(@PathVariable bikeId: Long): List<ServiceRecord> =
        repo.findAllByBikeIdOrderByPerformedAtDesc(bikeId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable bikeId: Long,
        @RequestBody body: ServiceRecordRequest
    ): ServiceRecord {
        val rec = ServiceRecord(
            bikeId = bikeId,
            componentId = body.componentId,
            performedAt = body.performedAt ?: Instant.now(),
            description = body.description,
            kmAtService = body.kmAtService,
            cost = body.cost,
            currency = body.currency ?: "CZK",
            note = body.note
        )
        return repo.save(rec)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable bikeId: Long,
        @PathVariable id: Long,
        @RequestBody body: ServiceRecordRequest
    ): ServiceRecord {
        val existing = repo.findByIdAndBikeId(id, bikeId)
            .orElseThrow { NoSuchElementException("ServiceRecord $id not found for bike $bikeId") }

        val updated = existing.copy(
            componentId = body.componentId,
            performedAt = body.performedAt ?: existing.performedAt,
            description = body.description,
            kmAtService = body.kmAtService,
            cost = body.cost,
            currency = body.currency ?: existing.currency,
            note = body.note
        )
        return repo.save(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable bikeId: Long,
        @PathVariable id: Long
    ) {
        val existing = repo.findByIdAndBikeId(id, bikeId)
            .orElseThrow { NoSuchElementException("ServiceRecord $id not found for bike $bikeId") }
        repo.delete(existing)
    }
}
