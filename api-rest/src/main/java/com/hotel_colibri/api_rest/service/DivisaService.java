package com.hotel_colibri.api_rest.service;

import com.hotel_colibri.api_rest.soap.DivisasClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DivisaService {

    private final DivisasClient divisas;

    public DivisaService(DivisasClient divisas) {
        this.divisas = divisas;
    }

    public List<String> listarMonedas() {
        return divisas.listarMonedas();
    }
}
