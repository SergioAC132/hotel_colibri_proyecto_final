package com.hotel_colibri.api_rest.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reserva en JSON-LD (schema.org/LodgingReservation). estadoHotel es propia. */
public record ReservaResponse(
        @JsonProperty("@context") String context,
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        String reservationId,
        String reservationStatus,
        LocalDate checkinTime,
        LocalDate checkoutTime,
        int numAdults,
        Persona underName,
        Habitacion reservationFor,
        BigDecimal totalPrice,
        String priceCurrency,
        String estadoHotel) {

    public record Persona(@JsonProperty("@type") String type, Long identifier, String name, String email) {
    }

    public record Habitacion(@JsonProperty("@type") String type, String identifier, String name) {
    }
}
