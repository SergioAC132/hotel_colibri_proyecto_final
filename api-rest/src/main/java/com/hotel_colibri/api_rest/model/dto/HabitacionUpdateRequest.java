package com.hotel_colibri.api_rest.model.dto;

import com.hotel_colibri.api_rest.model.entity.TipoHabitacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Actualizacion de habitacion. El numero no se modifica. */
public record HabitacionUpdateRequest(
        @Schema(example = "DOBLE")
        @NotNull(message = "es obligatorio (SENCILLA, DOBLE o SUITE)")
        TipoHabitacion tipo,

        @Schema(example = "2")
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 10, message = "no puede ser mayor que 10")
        Integer capacidad,

        @Schema(example = "1800.00", description = "Precio por noche en MXN")
        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.01", message = "debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "debe tener máximo 8 enteros y 2 decimales")
        BigDecimal precioNoche,

        @Schema(example = "true")
        @NotNull(message = "es obligatorio")
        Boolean activa) {
}
