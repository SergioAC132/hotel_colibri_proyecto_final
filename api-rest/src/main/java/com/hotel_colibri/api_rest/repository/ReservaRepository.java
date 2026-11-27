package com.hotel_colibri.api_rest.repository;

import com.hotel_colibri.api_rest.model.entity.EstadoReserva;
import com.hotel_colibri.api_rest.model.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Regla de no traslape: hay conflicto si otra reserva no cancelada de la misma habitacion
     * cumple entrada &lt; :salida AND salida &gt; :entrada. La salida de una puede coincidir con la
     * entrada de otra.
     */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.habitacion.id = :habitacionId
              and r.estado <> :cancelada
              and r.fechaEntrada < :salida
              and r.fechaSalida > :entrada
            """)
    boolean existeTraslape(@Param("habitacionId") Long habitacionId,
                           @Param("entrada") LocalDate entrada,
                           @Param("salida") LocalDate salida,
                           @Param("cancelada") EstadoReserva cancelada);

    default boolean existeTraslape(Long habitacionId, LocalDate entrada, LocalDate salida) {
        return existeTraslape(habitacionId, entrada, salida, EstadoReserva.CANCELADA);
    }

    @Query("""
            select r from Reserva r
            join fetch r.huesped
            join fetch r.habitacion
            where r.id = :id
            """)
    Optional<Reserva> buscarConDetalle(@Param("id") Long id);

    @Query("""
            select r from Reserva r
            join fetch r.huesped hu
            join fetch r.habitacion
            where (:estado is null or r.estado = :estado)
              and (:huespedId is null or hu.id = :huespedId)
            order by r.id
            """)
    List<Reserva> buscar(@Param("estado") EstadoReserva estado,
                         @Param("huespedId") Long huespedId);
}
