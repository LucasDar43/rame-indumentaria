package com.tudominio.rame_indumentaria.repository;

import com.tudominio.rame_indumentaria.model.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagenProductoRepository extends JpaRepository<ImagenProducto, Long> {
}
