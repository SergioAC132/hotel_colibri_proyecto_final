package com.hotel_colibri.api_rest.service;

import com.hotel_colibri.api_rest.model.dto.HabitacionRequest;
import com.hotel_colibri.api_rest.model.dto.HabitacionResponse;
import com.hotel_colibri.api_rest.model.dto.HabitacionUpdateRequest;
import com.hotel_colibri.api_rest.model.dto.JsonLdMapper;
import com.hotel_colibri.api_rest.model.entity.Habitacion;
import com.hotel_colibri.api_rest.exception.FechasInvalidasException;
import com.hotel_colibri.api_rest.exception.RecursoDuplicadoException;
import com.hotel_colibri.api_rest.exception.RecursoNoEncontradoException;
import com.hotel_colibri.api_rest.repository.HabitacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class HabitacionService {

    private final HabitacionRepository habitaciones;

    public HabitacionService(HabitacionRepository habitaciones) {
        this.habitaciones = habitaciones;
    }

    @Transactional
    public HabitacionResponse crear(HabitacionRequest req) {
        String numero = req.numero().trim();
        if (habitaciones.existsByNumero(numero)) {
            throw new RecursoDuplicadoException("Ya existe una habitación con el número " + numero + ".");
        }
        Habitacion h = new Habitacion(numero, req.tipo(), req.capacidad(),
                req.precioNoche().setScale(2, RoundingMode.HALF_UP));
        h.setActiva(req.activa() == null || req.activa());
        return JsonLdMapper.habitacion(habitaciones.save(h));
    }

    @Transactional(readOnly = true)
    public List<HabitacionResponse> listar(Boolean activa) {
        List<Habitacion> lista = activa == null
                ? habitaciones.findAllByOrderByNumeroAsc()
                : habitaciones.findByActivaOrderByNumeroAsc(activa);
        return lista.stream().map(JsonLdMapper::habitacion).toList();
    }

    @Transactional(readOnly = true)
    public HabitacionResponse obtener(Long id) {
        return JsonLdMapper.habitacion(buscar(id));
    }

    @Transactional
    public HabitacionResponse actualizar(Long id, HabitacionUpdateRequest req) {
        Habitacion h = buscar(id);
        h.setTipo(req.tipo());
        h.setCapacidad(req.capacidad());
        h.setPrecioNoche(req.precioNoche().setScale(2, RoundingMode.HALF_UP));
        h.setActiva(req.activa());
        return JsonLdMapper.habitacion(h);
    }

    /** Baja logica: la habitacion nunca se borra fisicamente. */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(id).setActiva(false);
    }

    @Transactional(readOnly = true)
    public List<HabitacionResponse> disponibles(LocalDate entrada, LocalDate salida, int huespedes) {
        if (!salida.isAfter(entrada)) {
            throw new FechasInvalidasException("La fecha de salida debe ser posterior a la fecha de entrada.");
        }
        return habitaciones.buscarDisponibles(entrada, salida, huespedes).stream()
                .map(JsonLdMapper::habitacion).toList();
    }

    private Habitacion buscar(Long id) {
        return habitaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la habitación con id " + id + "."));
    }
}
