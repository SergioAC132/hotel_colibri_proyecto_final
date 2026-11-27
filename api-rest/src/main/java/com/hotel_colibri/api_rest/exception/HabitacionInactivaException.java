package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 422: la habitacion esta dada de baja. */
public class HabitacionInactivaException extends ApiException {

    public HabitacionInactivaException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "habitacion-inactiva", "Habitación inactiva", mensaje);
    }
}
