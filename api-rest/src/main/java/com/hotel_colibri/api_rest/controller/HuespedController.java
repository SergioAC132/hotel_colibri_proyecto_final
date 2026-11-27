package com.hotel_colibri.api_rest.controller;

import com.hotel_colibri.api_rest.model.dto.HuespedRequest;
import com.hotel_colibri.api_rest.model.dto.HuespedResponse;
import com.hotel_colibri.api_rest.service.HuespedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/huespedes")
@Tag(name = "Huéspedes", description = "Registro de huéspedes")
public class HuespedController {

    private final HuespedService service;

    public HuespedController(HuespedService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un huésped (409 si el email ya existe)")
    public ResponseEntity<HuespedResponse> crear(@Valid @RequestBody HuespedRequest req) {
        HuespedResponse creado = service.crear(req);
        return ResponseEntity.created(URI.create(creado.id())).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar huéspedes")
    public List<HuespedResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un huésped por id")
    public HuespedResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre, email y teléfono de un huésped")
    public HuespedResponse actualizar(@PathVariable Long id, @Valid @RequestBody HuespedRequest req) {
        return service.actualizar(id, req);
    }
}
