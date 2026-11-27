package com.hotel_colibri.api_rest.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** Habitacion en JSON-LD (schema.org/HotelRoom). tipoHabitacion y activa son propias. */
public record HabitacionResponse(
        @JsonProperty("@context") String context,
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        String identifier,
        String name,
        Occupancy occupancy,
        Offer offers,
        String tipoHabitacion,
        boolean activa) {

    public record Occupancy(@JsonProperty("@type") String type, int maxValue) {
    }

    public record Offer(@JsonProperty("@type") String type, BigDecimal price, String priceCurrency) {
    }
}
