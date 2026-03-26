package com.tudominio.rame_indumentaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VarianteDTO {
    private Long id;
    private Long productoId;
    private String talle;
    private String color;
    private Integer stock;
    private String sku;
    private Boolean activo;
}
