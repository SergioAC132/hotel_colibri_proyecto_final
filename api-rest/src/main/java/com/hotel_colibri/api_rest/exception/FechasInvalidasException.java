package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 400: rango de fechas invalido. */
public class FechasInvalidasException extends ApiException {

    public FechasInvalidasException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "fechas-invalidas", "Fechas inválidas", mensaje);
    }
}
