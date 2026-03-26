package com.tudominio.rame_indumentaria.repository;

import com.tudominio.rame_indumentaria.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VarianteRepository extends JpaRepository<Variante, Long> {

    List<Variante> findByProductoId(Long productoId);

    List<Variante> findByProductoIdAndActivoTrue(Long productoId);

    Optional<Variante> findBySku(String sku);
}
