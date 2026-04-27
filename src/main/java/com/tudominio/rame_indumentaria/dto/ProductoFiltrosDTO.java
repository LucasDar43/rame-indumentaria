package com.tudominio.rame_indumentaria.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoFiltrosDTO {
    private String q;
    private String categoria;
    private String marca;
    private String color;
    private String talle;
    private String ordenar;
    // valores validos para ordenar:
    // "precio-asc", "precio-desc", "nombre-asc", "nombre-desc"
}
