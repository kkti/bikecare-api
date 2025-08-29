package com.bikecare.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "bikecare.health")
data class HealthProperties(
    var warnAt: Int = 75,
    var criticalAt: Int = 95
)
