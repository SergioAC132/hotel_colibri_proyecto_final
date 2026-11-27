package com.hotel_colibri.api_rest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Solicitud de reserva. No lleva precio: el total lo calcula siempre el servidor. */
public record ReservaRequest(
        @Schema(example = "1")
        @NotNull(message = "es obligatorio")
        Long huespedId,

        @Schema(example = "102")
        @NotBlank(message = "es obligatorio")
        String numeroHabitacion,

        @Schema(example = "2027-01-15", type = "string", format = "date")
        @NotNull(message = "es obligatoria (AAAA-MM-DD)")
        LocalDate fechaEntrada,

        @Schema(example = "2027-01-18", type = "string", format = "date")
        @NotNull(message = "es obligatoria (AAAA-MM-DD)")
        LocalDate fechaSalida,

        @Schema(example = "2")
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer numeroHuespedes) {
}
