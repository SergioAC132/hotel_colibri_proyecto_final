package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 422: el numero de huespedes supera la capacidad de la habitacion. */
public class CapacidadExcedidaException extends ApiException {

    public CapacidadExcedidaException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "capacidad-excedida", "Capacidad excedida", mensaje);
    }
}
