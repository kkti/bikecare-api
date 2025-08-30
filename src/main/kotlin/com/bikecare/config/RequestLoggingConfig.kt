package com.bikecare.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter

@Configuration
class RequestLoggingConfig {
    @Bean
    fun requestLoggingFilter(): CommonsRequestLoggingFilter =
        CommonsRequestLoggingFilter().apply {
            setIncludeQueryString(true)
            setIncludePayload(true)
            setIncludeClientInfo(true)
            setIncludeHeaders(false)
            setMaxPayloadLength(10000)
        }
}
