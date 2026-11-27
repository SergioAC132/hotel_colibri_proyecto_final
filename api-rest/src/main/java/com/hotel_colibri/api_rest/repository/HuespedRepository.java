package com.hotel_colibri.api_rest.repository;

import com.hotel_colibri.api_rest.model.entity.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Huesped> findAllByOrderByIdAsc();
}
