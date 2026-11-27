package com.hotel_colibri.api_rest.exception;

/**
 * Error de negocio informado por un servicio externo. {@code codigo} es el faultcode sin prefijo
 * de namespace, por ejemplo "Client.MonedaNoSoportada".
 */
public class SoapFaultException extends RuntimeException {

    private final String codigo;

    public SoapFaultException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
