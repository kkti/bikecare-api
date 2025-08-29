package com.bikecare.bikecomponent

import java.math.BigDecimal
import java.math.RoundingMode

object ComponentHealth {

    data class Metrics(
        val lifespanKm: BigDecimal,
        val usedKm: BigDecimal,
        val remainingKm: BigDecimal,
        val percentWear: Int
    )

    enum class WearStatus { OK, WARN, CRITICAL, EXPIRED }

    fun compute(
        installedOdometerKm: BigDecimal?,
        currentOdometerKm: BigDecimal?,
        lifespanOverride: BigDecimal?,
        defaultLifespan: BigDecimal?
    ): Metrics? {
        val installed = installedOdometerKm ?: return null
        val current = currentOdometerKm ?: return null
        val lifespan = lifespanOverride ?: defaultLifespan ?: return null

        val used = current.subtract(installed).coerceAtLeast(BigDecimal.ZERO)
        val remaining = lifespan.subtract(used).coerceAtLeast(BigDecimal.ZERO)

        val percent = if (lifespan > BigDecimal.ZERO) {
            used.multiply(BigDecimal(100))
                .divide(lifespan, 0, RoundingMode.HALF_UP)
                .toInt()
                .coerceIn(0, 100)
        } else 0

        return Metrics(lifespan, used, remaining, percent)
    }

    fun classify(metrics: Metrics, warnAt: Int = 80, criticalAt: Int = 95): WearStatus {
        if (metrics.percentWear >= 100 || metrics.remainingKm.compareTo(BigDecimal.ZERO) <= 0) {
            return WearStatus.EXPIRED
        }
        return when {
            metrics.percentWear >= criticalAt -> WearStatus.CRITICAL
            metrics.percentWear >= warnAt     -> WearStatus.WARN
            else                              -> WearStatus.OK
        }
    }
}
