package com.hotel_colibri.api_rest.repository;

import com.hotel_colibri.api_rest.model.entity.EstadoReserva;
import com.hotel_colibri.api_rest.model.entity.Habitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    boolean existsByNumero(String numero);

    Optional<Habitacion> findByNumero(String numero);

    List<Habitacion> findAllByOrderByNumeroAsc();

    List<Habitacion> findByActivaOrderByNumeroAsc(boolean activa);

    /** Obtiene la habitacion bloqueando su fila (SELECT ... FOR UPDATE) hasta que termine la transaccion. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.numero = :numero")
    Optional<Habitacion> findByNumeroParaActualizar(@Param("numero") String numero);

    /** Habitaciones activas, con capacidad suficiente y sin reservas no canceladas que se traslapen. */
    @Query("""
            select h from Habitacion h
            where h.activa = true
              and h.capacidad >= :huespedes
              and not exists (
                  select r.id from Reserva r
                  where r.habitacion = h
                    and r.estado <> :cancelada
                    and r.fechaEntrada < :salida
                    and r.fechaSalida > :entrada)
            order by h.numero
            """)
    List<Habitacion> buscarDisponibles(@Param("entrada") LocalDate entrada,
                                       @Param("salida") LocalDate salida,
                                       @Param("huespedes") int huespedes,
                                       @Param("cancelada") EstadoReserva cancelada);

    default List<Habitacion> buscarDisponibles(LocalDate entrada, LocalDate salida, int huespedes) {
        return buscarDisponibles(entrada, salida, huespedes, EstadoReserva.CANCELADA);
    }
}
