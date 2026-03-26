package com.tudominio.rame_indumentaria.repository;

import com.tudominio.rame_indumentaria.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrue();

    List<Producto> findByCategoriaAndActivoTrue(String categoria);

    List<Producto> findByMarcaAndActivoTrue(String marca);

    Page<Producto> findByActivoTrue(Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE " +
            "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(p.marca) LIKE LOWER(CONCAT('%', :busqueda, '%'))")
    List<Producto> buscar(@Param("busqueda") String busqueda);
}


