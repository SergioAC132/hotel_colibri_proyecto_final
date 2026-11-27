package com.hotel_colibri.api_rest.service;

import com.hotel_colibri.api_rest.model.dto.CotizacionResponse;
import com.hotel_colibri.api_rest.model.dto.JsonLdMapper;
import com.hotel_colibri.api_rest.model.dto.ReservaRequest;
import com.hotel_colibri.api_rest.model.dto.ReservaResponse;
import com.hotel_colibri.api_rest.model.entity.EstadoReserva;
import com.hotel_colibri.api_rest.model.entity.Habitacion;
import com.hotel_colibri.api_rest.model.entity.Huesped;
import com.hotel_colibri.api_rest.model.entity.Reserva;
import com.hotel_colibri.api_rest.exception.CapacidadExcedidaException;
import com.hotel_colibri.api_rest.exception.FechasInvalidasException;
import com.hotel_colibri.api_rest.exception.HabitacionInactivaException;
import com.hotel_colibri.api_rest.exception.RecursoNoEncontradoException;
import com.hotel_colibri.api_rest.exception.TraslapeReservaException;
import com.hotel_colibri.api_rest.repository.HabitacionRepository;
import com.hotel_colibri.api_rest.repository.HuespedRepository;
import com.hotel_colibri.api_rest.repository.ReservaRepository;
import com.hotel_colibri.api_rest.soap.DivisasClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final HabitacionRepository habitaciones;
    private final HuespedRepository huespedes;
    private final DivisasClient divisas;
    private final Clock reloj;

    public ReservaService(ReservaRepository reservas, HabitacionRepository habitaciones,
                          HuespedRepository huespedes, DivisasClient divisas, Clock reloj) {
        this.reservas = reservas;
        this.habitaciones = habitaciones;
        this.huespedes = huespedes;
        this.divisas = divisas;
        this.reloj = reloj;
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest req) {
        LocalDate entrada = req.fechaEntrada();
        LocalDate salida = req.fechaSalida();

        // 1. Fechas
        if (!salida.isAfter(entrada)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        if (entrada.isBefore(LocalDate.now(reloj))) {
            throw new FechasInvalidasException("La fecha de entrada no puede estar en el pasado.");
        }

        // 2. Existencia. La fila de la habitacion queda bloqueada (PESSIMISTIC_WRITE) hasta el commit,
        //    asi dos reservas simultaneas de la misma habitacion se validan una despues de otra.
        Huesped huesped = huespedes.findById(req.huespedId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el huésped con id " + req.huespedId() + "."));
        String numero = req.numeroHabitacion().trim();
        Habitacion habitacion = habitaciones.findByNumeroParaActualizar(numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación " + numero + "."));

        // 3. Habitacion activa
        if (!habitacion.isActiva()) {
            throw new HabitacionInactivaException("La habitación " + numero + " no está disponible para reservar.");
        }
        // 4. Capacidad
        if (req.numeroHuespedes() > habitacion.getCapacidad()) {
            throw new CapacidadExcedidaException("La habitación " + numero + " admite máximo "
                    + habitacion.getCapacidad() + " huésped(es); se solicitaron " + req.numeroHuespedes() + ".");
        }
        // 5. No traslape
        if (reservas.existeTraslape(habitacion.getId(), entrada, salida)) {
            throw new TraslapeReservaException("La habitación " + numero + " ya está reservada en parte del periodo "
                    + entrada + " a " + salida + ".");
        }

        // 6. Total calculado por el servidor
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        BigDecimal total = habitacion.getPrecioNoche()
                .multiply(BigDecimal.valueOf(noches))
                .setScale(2, RoundingMode.HALF_UP);

        Reserva r = new Reserva();
        r.setHuesped(huesped);
        r.setHabitacion(habitacion);
        r.setFechaEntrada(entrada);
        r.setFechaSalida(salida);
        r.setNumeroHuespedes(req.numeroHuespedes());
        r.setEstado(EstadoReserva.PENDIENTE);
        r.setTotalMxn(total);
        return JsonLdMapper.reserva(reservas.save(r));
    }

    @Transactional(readOnly = true)
    public ReservaResponse obtener(Long id) {
        return JsonLdMapper.reserva(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listar(EstadoReserva estado, Long huespedId) {
        return reservas.buscar(estado, huespedId).stream().map(JsonLdMapper::reserva).toList();
    }

    /** Cotiza el total de la reserva en otra moneda. La conversion la hace el servicio de divisas. */
    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(Long id, String moneda) {
        Reserva r = buscar(id);
        String destino = moneda.trim().toUpperCase(Locale.ROOT);
        BigDecimal precio = JsonLdMapper.MXN.equals(destino)
                ? r.getTotalMxn()
                : divisas.convertir(r.getTotalMxn(), JsonLdMapper.MXN, destino);
        return JsonLdMapper.cotizacion(r, precio, destino);
    }

    private Reserva buscar(Long id) {
        return reservas.buscarConDetalle(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva con id " + id + "."));
    }
}
