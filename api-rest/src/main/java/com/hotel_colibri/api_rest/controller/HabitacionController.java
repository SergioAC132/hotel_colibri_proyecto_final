package com.hotel_colibri.api_rest.controller;

import com.hotel_colibri.api_rest.model.dto.HabitacionRequest;
import com.hotel_colibri.api_rest.model.dto.HabitacionResponse;
import com.hotel_colibri.api_rest.model.dto.HabitacionUpdateRequest;
import com.hotel_colibri.api_rest.service.HabitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/habitaciones")
@Tag(name = "Habitaciones", description = "Catálogo de habitaciones y disponibilidad")
public class HabitacionController {

    private final HabitacionService service;

    public HabitacionController(HabitacionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar una habitación (409 si el número ya existe)")
    public ResponseEntity<HabitacionResponse> crear(@Valid @RequestBody HabitacionRequest req) {
        HabitacionResponse creada = service.crear(req);
        return ResponseEntity.created(URI.create(creada.id())).body(creada);
    }

    @GetMapping
    @Operation(summary = "Listar habitaciones, opcionalmente filtradas por activa")
    public List<HabitacionResponse> listar(
            @Parameter(description = "true = solo activas, false = solo dadas de baja")
            @RequestParam(required = false) Boolean activa) {
        return service.listar(activa);
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Habitaciones activas con capacidad suficiente y sin reservas en el periodo")
    public List<HabitacionResponse> disponibles(
            @Parameter(example = "2027-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @Parameter(example = "2027-01-18") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @Parameter(example = "2") @RequestParam @Min(value = 1, message = "debe ser al menos 1")
            @Max(value = 10, message = "no puede ser mayor que 10") int huespedes) {
        return service.disponibles(entrada, salida, huespedes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una habitación por id")
    public HabitacionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo, capacidad, precio por noche y estado activo")
    public HabitacionResponse actualizar(@PathVariable Long id, @Valid @RequestBody HabitacionUpdateRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja una habitación (baja lógica: activa=false)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
