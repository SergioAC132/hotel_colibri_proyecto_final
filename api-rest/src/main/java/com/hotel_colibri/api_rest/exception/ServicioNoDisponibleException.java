package com.hotel_colibri.api_rest.exception;

/** El servicio externo no respondio (conexion rechazada, timeout, etc.). */
public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, Throwable causa) {
        super("El servicio de " + servicio + " no está disponible. Intente más tarde.", causa);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
