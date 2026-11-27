package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/** 404: el recurso solicitado no existe. */
public class RecursoNoEncontradoException extends ApiException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, "recurso-no-encontrado", "Recurso no encontrado", mensaje);
    }
}
