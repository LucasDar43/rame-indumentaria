package com.tudominio.rame_indumentaria.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String marca;
    private String categoria;
    private String subcategoria;
    private String imagenUrl;
    private List<ImagenProductoDTO> imagenes;   // ← antes era List<String>
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
