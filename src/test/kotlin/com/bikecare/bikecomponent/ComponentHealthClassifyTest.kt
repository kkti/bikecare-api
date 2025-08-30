package com.bikecare.bikecomponent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ComponentHealthClassifyTest {

    private fun m(lifespan: Int, used: Int): ComponentHealth.Metrics {
        val ls = BigDecimal(lifespan)
        val us = BigDecimal(used)
        val rem = ls.subtract(us).coerceAtLeast(BigDecimal.ZERO)
        val pct = if (lifespan > 0) (used * 100 / lifespan).coerceIn(0, 100) else 0
        return ComponentHealth.Metrics(
            lifespanKm = ls,
            usedKm = us.coerceAtLeast(BigDecimal.ZERO),
            remainingKm = rem,
            percentWear = pct
        )
    }

    @Test
    @DisplayName("classify: OK/WARN/CRITICAL/EXPIRED dle prahů")
    fun classify_states() {
        // warn=75, critical=95
        val warnAt = 75
        val criticalAt = 95

        assertEquals(ComponentHealth.WearStatus.OK,
            ComponentHealth.classify(m(2500, 1200), warnAt, criticalAt))

        assertEquals(ComponentHealth.WearStatus.WARN,
            ComponentHealth.classify(m(2500, 1900), warnAt, criticalAt)) // 76 %

        assertEquals(ComponentHealth.WearStatus.CRITICAL,
            ComponentHealth.classify(m(2500, 2400), warnAt, criticalAt)) // 96 %

        // EXPIRED: zbyva 0 a percent=100
        assertEquals(ComponentHealth.WearStatus.EXPIRED,
            ComponentHealth.classify(m(2500, 2500), warnAt, criticalAt))
    }
}
