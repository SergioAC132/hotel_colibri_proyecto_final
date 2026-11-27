package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 409: la habitacion ya tiene una reserva en esas fechas. */
public class TraslapeReservaException extends ApiException {

    public TraslapeReservaException(String mensaje) {
        super(HttpStatus.CONFLICT, "traslape-reserva", "Habitación no disponible", mensaje);
    }
}
