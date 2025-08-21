package com.bikecare.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun bikecareApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("bikecare")
            .packagesToScan(
                "com.bikecare.auth",
                "com.bikecare.bike",
                "com.bikecare.bikecomponent",
                "com.bikecare.bikecomponent.history", // nově: historie
                "com.bikecare.componenttype",
                "com.bikecare.odometer"               // nově: odometer
            )
            .pathsToMatch("/api/**")
            .build()

    @Bean
    fun openAPI(): OpenAPI {
        val bearer = SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
        return OpenAPI()
            .info(
                Info()
                    .title("BikeCare API")
                    .version("0.1.0")
                    .description("Backend API pro evidenci kol & komponent")
            )
            .components(Components().addSecuritySchemes("bearerAuth", bearer))
            .addSecurityItem(SecurityRequirement().addList("bearerAuth"))
    }
}
