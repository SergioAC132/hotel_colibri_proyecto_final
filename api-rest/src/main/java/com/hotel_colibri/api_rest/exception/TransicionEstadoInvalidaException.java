package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 409: la reserva no admite la operacion en su estado actual. */
public class TransicionEstadoInvalidaException extends ApiException {

    public TransicionEstadoInvalidaException(String mensaje) {
        super(HttpStatus.CONFLICT, "transicion-estado-invalida", "Transición de estado inválida", mensaje);
    }
}
