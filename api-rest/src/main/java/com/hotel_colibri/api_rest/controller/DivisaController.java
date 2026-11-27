package com.hotel_colibri.api_rest.controller;

import com.hotel_colibri.api_rest.service.DivisaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/divisas")
@Tag(name = "Divisas", description = "Monedas aceptadas por el hotel")
public class DivisaController {

    private final DivisaService service;

    public DivisaController(DivisaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar los códigos de moneda soportados (ISO 4217)")
    public List<String> listar() {
        return service.listarMonedas();
    }
}
