package com.hotel_colibri.api_rest.model.dto;

import com.hotel_colibri.api_rest.model.entity.EstadoReserva;
import com.hotel_colibri.api_rest.model.entity.Habitacion;
import com.hotel_colibri.api_rest.model.entity.Huesped;
import com.hotel_colibri.api_rest.model.entity.Reserva;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte entidades a respuestas JSON-LD con vocabulario de schema.org. */
public final class JsonLdMapper {

    public static final String CONTEXTO = "https://schema.org";
    public static final String MXN = "MXN";

    private JsonLdMapper() {
    }

    public static HabitacionResponse habitacion(Habitacion h) {
        return new HabitacionResponse(
                CONTEXTO, "HotelRoom", "/api/v1/habitaciones/" + h.getId(),
                h.getNumero(),
                nombreHabitacion(h),
                new HabitacionResponse.Occupancy("QuantitativeValue", h.getCapacidad()),
                new HabitacionResponse.Offer("Offer", dinero(h.getPrecioNoche()), MXN),
                h.getTipo().name(),
                h.isActiva());
    }

    public static HuespedResponse huesped(Huesped h) {
        return new HuespedResponse(
                CONTEXTO, "Person", "/api/v1/huespedes/" + h.getId(),
                h.getId(), h.getNombre(), h.getEmail(), h.getTelefono());
    }

    public static ReservaResponse reserva(Reserva r) {
        Huesped hu = r.getHuesped();
        Habitacion ha = r.getHabitacion();
        return new ReservaResponse(
                CONTEXTO, "LodgingReservation", "/api/v1/reservas/" + r.getId(),
                folioReserva(r),
                estadoSchemaOrg(r.getEstado()),
                r.getFechaEntrada(),
                r.getFechaSalida(),
                r.getNumeroHuespedes(),
                new ReservaResponse.Persona("Person", hu.getId(), hu.getNombre(), hu.getEmail()),
                new ReservaResponse.Habitacion("HotelRoom", ha.getNumero(), nombreHabitacion(ha)),
                dinero(r.getTotalMxn()),
                MXN,
                r.getEstado().name());
    }

    public static CotizacionResponse cotizacion(Reserva r, BigDecimal precio, String moneda) {
        return new CotizacionResponse(CONTEXTO, "PriceSpecification",
                dinero(precio), moneda, folioReserva(r), dinero(r.getTotalMxn()));
    }

    public static String folioReserva(Reserva r) {
        return "RES-" + r.getId();
    }

    public static String estadoSchemaOrg(EstadoReserva estado) {
        return "https://schema.org/" + switch (estado) {
            case PENDIENTE -> "ReservationPending";
            case CONFIRMADA, EN_CURSO, FINALIZADA -> "ReservationConfirmed";
            case CANCELADA -> "ReservationCancelled";
        };
    }

    static String nombreHabitacion(Habitacion h) {
        return "Habitación " + h.getNumero() + " · " + h.getTipo().name();
    }

    /** Dinero siempre con 2 decimales. */
    public static BigDecimal dinero(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
