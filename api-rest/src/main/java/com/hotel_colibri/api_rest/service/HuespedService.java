package com.hotel_colibri.api_rest.service;

import com.hotel_colibri.api_rest.model.dto.HuespedRequest;
import com.hotel_colibri.api_rest.model.dto.HuespedResponse;
import com.hotel_colibri.api_rest.model.dto.JsonLdMapper;
import com.hotel_colibri.api_rest.model.entity.Huesped;
import com.hotel_colibri.api_rest.exception.RecursoDuplicadoException;
import com.hotel_colibri.api_rest.exception.RecursoNoEncontradoException;
import com.hotel_colibri.api_rest.repository.HuespedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class HuespedService {

    private final HuespedRepository huespedes;

    public HuespedService(HuespedRepository huespedes) {
        this.huespedes = huespedes;
    }

    @Transactional
    public HuespedResponse crear(HuespedRequest req) {
        String email = normalizarEmail(req.email());
        if (huespedes.existsByEmailIgnoreCase(email)) {
            throw new RecursoDuplicadoException("Ya existe un huésped con el email " + email + ".");
        }
        Huesped h = new Huesped(req.nombre().trim(), email, limpiar(req.telefono()));
        return JsonLdMapper.huesped(huespedes.save(h));
    }

    @Transactional(readOnly = true)
    public List<HuespedResponse> listar() {
        return huespedes.findAllByOrderByIdAsc().stream().map(JsonLdMapper::huesped).toList();
    }

    @Transactional(readOnly = true)
    public HuespedResponse obtener(Long id) {
        return JsonLdMapper.huesped(buscar(id));
    }

    @Transactional
    public HuespedResponse actualizar(Long id, HuespedRequest req) {
        Huesped h = buscar(id);
        String email = normalizarEmail(req.email());
        if (huespedes.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RecursoDuplicadoException("Ya existe otro huésped con el email " + email + ".");
        }
        h.setNombre(req.nombre().trim());
        h.setEmail(email);
        h.setTelefono(limpiar(req.telefono()));
        return JsonLdMapper.huesped(h);
    }

    private Huesped buscar(Long id) {
        return huespedes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el huésped con id " + id + "."));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
