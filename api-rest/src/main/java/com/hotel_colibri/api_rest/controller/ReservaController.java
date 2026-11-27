package com.hotel_colibri.api_rest.controller;

import com.hotel_colibri.api_rest.model.dto.CotizacionResponse;
import com.hotel_colibri.api_rest.model.dto.ReservaRequest;
import com.hotel_colibri.api_rest.model.dto.ReservaResponse;
import com.hotel_colibri.api_rest.model.entity.EstadoReserva;
import com.hotel_colibri.api_rest.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas", description = "Reservas y cotizaciones")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear una reserva PENDIENTE (el total lo calcula el servidor)")
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest req) {
        ReservaResponse creada = service.crear(req);
        return ResponseEntity.created(URI.create(creada.id())).body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una reserva por id")
    public ReservaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    @Operation(summary = "Listar reservas, opcionalmente por estado y/o huésped")
    public List<ReservaResponse> listar(
            @Parameter(example = "PENDIENTE") @RequestParam(required = false) EstadoReserva estado,
            @Parameter(example = "1") @RequestParam(required = false) Long huespedId) {
        return service.listar(estado, huespedId);
    }

    @GetMapping("/{id}/cotizacion")
    @Operation(summary = "Cotizar el total de la reserva en otra moneda (MXN, USD, EUR, CAD)")
    public CotizacionResponse cotizar(
            @PathVariable Long id,
            @Parameter(example = "USD") @RequestParam
            @Pattern(regexp = "^[A-Za-z]{3}$", message = "debe ser un código de moneda de 3 letras") String moneda) {
        return service.cotizar(id, moneda);
    }
}
