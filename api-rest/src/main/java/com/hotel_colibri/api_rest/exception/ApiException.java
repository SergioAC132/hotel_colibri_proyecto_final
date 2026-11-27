package com.hotel_colibri.api_rest.exception;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que se traduce a un ProblemDetail (RFC 9457).
 * {@code tipo} es el sufijo del URI "type" y {@code titulo} el "title".
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String tipo;
    private final String titulo;

    protected ApiException(HttpStatus status, String tipo, String titulo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.tipo = tipo;
        this.titulo = titulo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }
}
