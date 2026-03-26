package com.tudominio.rame_indumentaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double precio;
    private String marca;
    private String categoria;
    private String subcategoria;
    private List<String> imagenes;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private String imagenUrl;
}