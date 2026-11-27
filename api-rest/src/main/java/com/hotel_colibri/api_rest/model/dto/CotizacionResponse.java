package com.hotel_colibri.api_rest.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Cotizacion en JSON-LD (schema.org/PriceSpecification). reservationId y totalMxn son propias. */
public record CotizacionResponse(
        @JsonProperty("@context") String context,
        @JsonProperty("@type") String type,
        BigDecimal price,
        String priceCurrency,
        String reservationId,
        BigDecimal totalMxn) {
}
