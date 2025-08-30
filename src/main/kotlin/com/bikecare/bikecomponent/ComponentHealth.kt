package com.bikecare.bikecomponent

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Jednoduchá klasifikace stavu komponenty podle využití (usage %).
 *
 * Výchozí prahy:
 *  - OK       : usage < 0.70  (70 % životnosti)
 *  - WARNING  : 0.70 <= usage < 0.90
 *  - REPLACE  : usage >= 0.90
 *  - UNKNOWN  : nelze spočítat (chybí data nebo lifespan <= 0)
 *
 * Volání:
 *  - ComponentHealth.classify(usagePercent)
 *  - ComponentHealth.classify(usedKm, lifespanKm)
 */
enum class ComponentHealth {
    OK, WARNING, REPLACE, UNKNOWN;

    companion object {
        /**
         * Klasifikace přímo z procent využití (0.0 .. 1.0).
         * Hodnoty mimo rozsah ořízne do <0,1>.
         */
        @JvmStatic
        fun classify(usagePercent: Double?): ComponentHealth {
            if (usagePercent == null || usagePercent.isNaN()) return UNKNOWN
            val u = usagePercent.coerceIn(0.0, 1.0)
            return when {
                u < 0.70 -> OK
                u < 0.90 -> WARNING
                else     -> REPLACE
            }
        }

        /**
         * Klasifikace z km: usedKm / lifespanKm.
         * Pokud lifespanKm <= 0 nebo některá hodnota chybí -> UNKNOWN.
         */
        @JvmStatic
        fun classify(usedKm: BigDecimal?, lifespanKm: BigDecimal?): ComponentHealth {
            if (usedKm == null || lifespanKm == null) return UNKNOWN
            if (lifespanKm <= BigDecimal.ZERO) return UNKNOWN

            val usage = try {
                usedKm.divide(lifespanKm, 8, RoundingMode.HALF_UP).toDouble()
            } catch (_: ArithmeticException) {
                return UNKNOWN
            }
            return classify(usage)
        }
    }
}
