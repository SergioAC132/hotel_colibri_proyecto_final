package com.hotel_colibri.api_rest.config;

import com.hotel_colibri.api_rest.model.entity.Habitacion;
import com.hotel_colibri.api_rest.model.entity.Huesped;
import com.hotel_colibri.api_rest.model.entity.TipoHabitacion;
import com.hotel_colibri.api_rest.repository.HabitacionRepository;
import com.hotel_colibri.api_rest.repository.HuespedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Datos iniciales. Idempotente: solo inserta lo que todavia no existe. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final HabitacionRepository habitaciones;
    private final HuespedRepository huespedes;

    public DataSeeder(HabitacionRepository habitaciones, HuespedRepository huespedes) {
        this.habitaciones = habitaciones;
        this.huespedes = huespedes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        habitacion("101", TipoHabitacion.SENCILLA, 1, "1200.00");
        habitacion("102", TipoHabitacion.DOBLE, 2, "1800.00");
        habitacion("201", TipoHabitacion.SUITE, 4, "3500.00");

        if (!huespedes.existsByEmailIgnoreCase("ana@correo.mx")) {
            huespedes.save(new Huesped("Ana Torres", "ana@correo.mx", "5512345678"));
            log.info("Seed: huesped ana@correo.mx creado");
        }
    }

    private void habitacion(String numero, TipoHabitacion tipo, int capacidad, String precio) {
        if (!habitaciones.existsByNumero(numero)) {
            habitaciones.save(new Habitacion(numero, tipo, capacidad, new BigDecimal(precio)));
            log.info("Seed: habitacion {} creada", numero);
        }
    }
}
