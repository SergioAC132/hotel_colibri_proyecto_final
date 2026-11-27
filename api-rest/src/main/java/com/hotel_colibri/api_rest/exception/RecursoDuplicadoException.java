package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 409: numero de habitacion o email ya registrado. */
public class RecursoDuplicadoException extends ApiException {

    public RecursoDuplicadoException(String mensaje) {
        super(HttpStatus.CONFLICT, "recurso-duplicado", "Recurso duplicado", mensaje);
    }
}
