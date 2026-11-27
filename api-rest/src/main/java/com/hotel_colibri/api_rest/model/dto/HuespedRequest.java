package com.hotel_colibri.api_rest.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HuespedRequest(
        @Schema(example = "Luis Ramírez")
        @NotBlank(message = "es obligatorio")
        @Size(max = 120, message = "no puede exceder 120 caracteres")
        String nombre,

        @Schema(example = "luis@correo.mx")
        @NotBlank(message = "es obligatorio")
        @Email(message = "no es un correo válido")
        @Size(max = 150, message = "no puede exceder 150 caracteres")
        String email,

        @Schema(example = "5598765432")
        @Pattern(regexp = "^\\+?[0-9 -]{7,20}$", message = "debe tener entre 7 y 20 dígitos")
        String telefono) {
}
