package com.hotel_colibri.api_rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    /** Reloj del hotel (hora de la Ciudad de Mexico); inyectable para pruebas. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Mexico_City"));
    }

    @Bean
    public OpenAPI hotelColibriOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API Hotel Colibrí")
                .version("v1")
                .description("Habitaciones, huéspedes, reservas, cotizaciones y divisas. "
                        + "Las respuestas usan JSON-LD con vocabulario de schema.org."));
    }
}
