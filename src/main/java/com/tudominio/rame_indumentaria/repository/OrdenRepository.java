package com.tudominio.rame_indumentaria.repository;

import com.tudominio.rame_indumentaria.model.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {
    Optional<Orden> findByMpPreferenceId(String mpPreferenceId);
    Optional<Orden> findByMpPaymentId(String mpPaymentId);
    Page<Orden> findAllByOrderByFechaCreacionDesc(Pageable pageable);
}
