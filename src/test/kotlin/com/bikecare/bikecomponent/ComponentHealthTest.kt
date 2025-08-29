package com.bikecare.bikecomponent

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ComponentHealthTest {

    @Test
    @DisplayName("compute returns metrics with override lifespan")
    fun compute_returns_metrics_with_override_lifespan() {
        val m = ComponentHealth.compute(
            installedOdometerKm = bd(1200),
            currentOdometerKm   = bd(3100),
            lifespanOverride    = bd(2500),
            defaultLifespan     = bd(3000)
        )
        assertNotNull(m)
        m!!
        assertEquals(bd(2500), m.lifespanKm)
        assertEquals(bd(1900), m.usedKm)
        assertEquals(bd(600),  m.remainingKm)
        assertEquals(76,       m.percentWear)
        assertEquals(ComponentHealth.WearStatus.WARN, ComponentHealth.classify(m, warnAt = 75, criticalAt = 95))
    }

    @Test
    @DisplayName("compute falls back to default lifespan when override is null")
    fun compute_falls_back_to_default_when_override_is_null() {
        val m = ComponentHealth.compute(
            installedOdometerKm = bd(100),
            currentOdometerKm   = bd(600),
            lifespanOverride    = null,
            defaultLifespan     = bd(1000)
        )
        assertNotNull(m)
        m!!
        assertEquals(bd(1000), m.lifespanKm)
        assertEquals(bd(500),  m.usedKm)
        assertEquals(bd(500),  m.remainingKm)
        assertEquals(50,       m.percentWear)
        assertEquals(ComponentHealth.WearStatus.OK, ComponentHealth.classify(m))
    }

    @Test
    @DisplayName("compute returns null when installed or current is missing")
    fun compute_returns_null_when_installed_or_current_is_missing() {
        assertNull(ComponentHealth.compute(null, bd(100), bd(1000), null))
        assertNull(ComponentHealth.compute(bd(0), null, bd(1000), null))
    }

    @Test
    @DisplayName("EXPIRED when remaining == 0 and percent >= 100")
    fun expired_when_remaining_zero_and_percent_100() {
        val m = ComponentHealth.compute(
            installedOdometerKm = bd(0),
            currentOdometerKm   = bd(2500),
            lifespanOverride    = bd(2500),
            defaultLifespan     = null
        )
        assertNotNull(m)
        m!!
        assertEquals(100, m.percentWear)
        assertEquals(ComponentHealth.WearStatus.EXPIRED, ComponentHealth.classify(m))
    }

    private fun bd(n: Int) = BigDecimal(n)
}
