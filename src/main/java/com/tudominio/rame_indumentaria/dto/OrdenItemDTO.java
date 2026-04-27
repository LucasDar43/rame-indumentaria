package com.tudominio.rame_indumentaria.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenItemDTO {
    private Long productoId;
    private Long varianteId;
    private String nombreProducto;
    private String talle;
    private String color;
    private String imagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
